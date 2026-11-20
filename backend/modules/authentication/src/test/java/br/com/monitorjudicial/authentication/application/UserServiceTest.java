package br.com.monitorjudicial.authentication.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.monitorjudicial.authentication.domain.User;
import br.com.monitorjudicial.authentication.domain.port.PasswordHasher;
import br.com.monitorjudicial.authentication.domain.port.RefreshTokenRepository;
import br.com.monitorjudicial.authentication.domain.port.UserRepository;
import br.com.monitorjudicial.clients.api.ClientDirectory;
import br.com.monitorjudicial.clients.api.ClientSummary;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import br.com.monitorjudicial.shared.audit.Auditor;
import br.com.monitorjudicial.shared.domain.ConflictException;
import br.com.monitorjudicial.shared.domain.Email;
import br.com.monitorjudicial.shared.domain.ResourceNotFoundException;
import br.com.monitorjudicial.shared.domain.UnauthorizedException;
import br.com.monitorjudicial.shared.security.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UserServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
    private final PasswordHasher hasher = mock(PasswordHasher.class);
    private final ClientDirectory clients = mock(ClientDirectory.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-23T10:00:00Z"), ZoneOffset.UTC);
    private final UserService service = new UserService(users, refreshTokens, hasher, clients, mock(Auditor.class), clock);

    @Test
    void createsCustomerLinkedToExistingClient() {
        UUID client = UUID.randomUUID();
        when(clients.findById(client)).thenReturn(Optional.of(
                new ClientSummary(client, "C", "c@x.com", null, true, false, true)));
        when(hasher.hash("senha-forte-123")).thenReturn("bcrypt");
        when(users.save(any())).thenAnswer(i -> i.getArgument(0));

        UserView view = service.create(new CreateUserCommand("Cliente", "c@x.com", "senha-forte-123", Role.CUSTOMER,
                client));

        assertThat(view.role()).isEqualTo(Role.CUSTOMER);
        assertThat(view.clientId()).isEqualTo(client);
    }

    @Test
    void rejectsDuplicateEmailAndUnknownClient() {
        when(users.existsByEmail(new Email("dup@x.com"))).thenReturn(true);
        assertThatThrownBy(() -> service.create(
                new CreateUserCommand("D", "dup@x.com", "senha-forte-123", Role.ADMIN, null)))
                .isInstanceOf(ConflictException.class);

        UUID missing = UUID.randomUUID();
        when(clients.findById(missing)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(
                new CreateUserCommand("C", "c@x.com", "senha-forte-123", Role.CUSTOMER, missing)))
                .extracting("code").isEqualTo("client-not-found");
    }

    @ParameterizedTest
    @ValueSource(strings = {"curta1", "somenteletrasaqui", "123456789012345"})
    void rejectsWeakPasswords(String password) {
        assertThatThrownBy(() -> UserService.validatePassword(password)).extracting("code").isEqualTo("weak-password");
    }

    @Test
    void getAndList() {
        User user = User.create("A", new Email("a@x.com"), "h", Role.ADMIN, null, clock.instant());
        when(users.findById(user.id())).thenReturn(Optional.of(user));
        when(users.findAll(PageQuery.of(0, 20))).thenReturn(PageResult.of(List.of(user), 0, 20, 1));
        assertThat(service.get(user.id()).email()).isEqualTo("a@x.com");
        assertThat(service.list(PageQuery.of(0, 20)).totalElements()).isEqualTo(1);
        assertThatThrownBy(() -> service.get(UUID.randomUUID())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void setActivePreventsSelfDeactivationAndRevokesTokens() {
        User user = User.create("A", new Email("a@x.com"), "h", Role.MANAGER, null, clock.instant());
        when(users.findById(user.id())).thenReturn(Optional.of(user));
        when(users.save(any())).thenAnswer(i -> i.getArgument(0));

        assertThatThrownBy(() -> service.setActive(user.id(), false, user.id()))
                .extracting("code").isEqualTo("cannot-deactivate-self");

        UserView deactivated = service.setActive(user.id(), false, UUID.randomUUID());
        assertThat(deactivated.active()).isFalse();
        verify(refreshTokens).revokeAllForUser(user.id(), clock.instant());

        UserView reactivated = service.setActive(user.id(), true, UUID.randomUUID());
        assertThat(reactivated.active()).isTrue();
    }

    @Test
    void changePasswordValidatesCurrentAndRevokesTokens() {
        User user = User.create("A", new Email("a@x.com"), "old-hash", Role.ADMIN, null, clock.instant());
        when(users.findById(user.id())).thenReturn(Optional.of(user));
        when(hasher.matches("errada", "old-hash")).thenReturn(false);
        when(hasher.matches("atual-forte-123", "old-hash")).thenReturn(true);
        when(hasher.matches("nova-forte-456", "old-hash")).thenReturn(false);
        when(hasher.hash("nova-forte-456")).thenReturn("new-hash");
        when(users.save(any())).thenAnswer(i -> i.getArgument(0));

        assertThatThrownBy(() -> service.changePassword(user.id(), "errada", "nova-forte-456"))
                .isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> service.changePassword(user.id(), "atual-forte-123", "atual-forte-123"))
                .extracting("code").isEqualTo("password-unchanged");

        service.changePassword(user.id(), "atual-forte-123", "nova-forte-456");
        verify(refreshTokens).revokeAllForUser(user.id(), clock.instant());
    }
}
