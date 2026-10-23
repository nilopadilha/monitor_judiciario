package br.com.monitorjudicial.authentication.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.monitorjudicial.authentication.domain.RefreshToken;
import br.com.monitorjudicial.authentication.domain.User;
import br.com.monitorjudicial.authentication.domain.port.AccessTokenIssuer;
import br.com.monitorjudicial.authentication.domain.port.PasswordHasher;
import br.com.monitorjudicial.authentication.domain.port.RefreshTokenRepository;
import br.com.monitorjudicial.authentication.domain.port.UserRepository;
import br.com.monitorjudicial.shared.audit.Auditor;
import br.com.monitorjudicial.shared.domain.Email;
import br.com.monitorjudicial.shared.domain.UnauthorizedException;
import br.com.monitorjudicial.shared.security.Role;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AuthenticationServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final RefreshTokenRepository tokens = mock(RefreshTokenRepository.class);
    private final PasswordHasher hasher = mock(PasswordHasher.class);
    private final AccessTokenIssuer issuer = mock(AccessTokenIssuer.class);
    private final Auditor auditor = mock(Auditor.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-23T10:00:00Z"), ZoneOffset.UTC);
    private final AuthenticationService service = new AuthenticationService(users, tokens, hasher, issuer, auditor,
            clock, Duration.ofDays(7));

    private User user;

    @BeforeEach
    void setUp() {
        user = User.create("Gestor", new Email("gestor@x.com"), "bcrypt", Role.MANAGER, null, clock.instant());
        when(issuer.issue(any())).thenReturn("jwt");
        when(issuer.validity()).thenReturn(Duration.ofMinutes(15));
        when(tokens.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void authenticatesAndStoresOnlyTheHashOfTheRefreshToken() {
        when(users.findByEmail(new Email("gestor@x.com"))).thenReturn(Optional.of(user));
        when(hasher.matches("senha", "bcrypt")).thenReturn(true);

        TokenPair pair = service.authenticate("GESTOR@x.com", "senha");

        assertThat(pair.accessToken()).isEqualTo("jwt");
        assertThat(pair.tokenType()).isEqualTo("Bearer");
        assertThat(pair.expiresInSeconds()).isEqualTo(900);
        var captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(tokens).save(captor.capture());
        assertThat(captor.getValue().tokenHash()).isEqualTo(AuthenticationService.hash(pair.refreshToken()))
                .isNotEqualTo(pair.refreshToken());
        assertThat(captor.getValue().expiresAt()).isEqualTo(clock.instant().plus(Duration.ofDays(7)));
        verify(auditor).record(eq("LOGIN"), eq("user"), eq(user.id()), anyMap());
    }

    @Test
    void wrongPasswordUnknownUserAndMalformedEmailShareTheSameError() {
        when(users.findByEmail(any())).thenReturn(Optional.of(user));
        when(hasher.matches(any(), any())).thenReturn(false);
        assertThatThrownBy(() -> service.authenticate("gestor@x.com", "errada"))
                .isInstanceOf(UnauthorizedException.class).hasMessage("E-mail ou senha inválidos.");
        assertThatThrownBy(() -> service.authenticate("nao-e-email", "x"))
                .isInstanceOf(UnauthorizedException.class).hasMessage("E-mail ou senha inválidos.");
    }

    @Test
    void refreshRotatesTokenWithinTheSameFamily() {
        UUID family = UUID.randomUUID();
        RefreshToken current = RefreshToken.issue(user.id(), AuthenticationService.hash("raw"), family,
                clock.instant().minusSeconds(60), clock.instant().plusSeconds(3600));
        when(tokens.findByTokenHash(AuthenticationService.hash("raw"))).thenReturn(Optional.of(current));
        when(users.findById(user.id())).thenReturn(Optional.of(user));

        TokenPair pair = service.refresh("raw");

        assertThat(pair.refreshToken()).isNotEqualTo("raw");
        assertThat(current.isRevoked()).isTrue();
        var captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(tokens, times(2)).save(captor.capture());
        RefreshToken successor = captor.getAllValues().get(1);
        assertThat(successor.familyId()).isEqualTo(family);
        assertThat(current.replacedById()).isEqualTo(successor.id());
    }

    @Test
    void reusingARotatedTokenRevokesTheWholeFamily() {
        RefreshToken stolen = RefreshToken.issue(user.id(), AuthenticationService.hash("old"), UUID.randomUUID(),
                clock.instant(), clock.instant().plusSeconds(3600));
        stolen.rotateTo(UUID.randomUUID(), clock.instant());
        when(tokens.findByTokenHash(AuthenticationService.hash("old"))).thenReturn(Optional.of(stolen));

        assertThatThrownBy(() -> service.refresh("old"))
                .isInstanceOf(UnauthorizedException.class).extracting("code").isEqualTo("refresh-token-reused");
        verify(tokens).revokeFamily(stolen.familyId(), clock.instant());
        verify(auditor).record(eq("REFRESH_TOKEN_REUSE"), eq("user"), eq(user.id()), anyMap());
    }

    @Test
    void expiredUnknownOrInactiveUserIsRejected() {
        RefreshToken expired = RefreshToken.issue(user.id(), AuthenticationService.hash("exp"), UUID.randomUUID(),
                clock.instant().minusSeconds(7200), clock.instant().minusSeconds(1));
        when(tokens.findByTokenHash(AuthenticationService.hash("exp"))).thenReturn(Optional.of(expired));
        assertThatThrownBy(() -> service.refresh("exp")).extracting("code").isEqualTo("refresh-token-expired");

        assertThatThrownBy(() -> service.refresh("desconhecido")).extracting("code").isEqualTo("invalid-refresh-token");

        RefreshToken valid = RefreshToken.issue(user.id(), AuthenticationService.hash("ok"), UUID.randomUUID(),
                clock.instant(), clock.instant().plusSeconds(60));
        when(tokens.findByTokenHash(AuthenticationService.hash("ok"))).thenReturn(Optional.of(valid));
        when(users.findById(user.id())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.refresh("ok")).extracting("code").isEqualTo("inactive-user");
    }

    @Test
    void revokeRevokesFamilyAndIgnoresUnknownTokens() {
        RefreshToken token = RefreshToken.issue(user.id(), AuthenticationService.hash("r"), UUID.randomUUID(),
                clock.instant(), clock.instant().plusSeconds(60));
        when(tokens.findByTokenHash(AuthenticationService.hash("r"))).thenReturn(Optional.of(token));
        service.revoke("r");
        verify(tokens).revokeFamily(token.familyId(), clock.instant());

        service.revoke("x");
        verify(tokens, never()).revokeFamily(eq(UUID.fromString("00000000-0000-0000-0000-000000000000")), any());
    }
}
