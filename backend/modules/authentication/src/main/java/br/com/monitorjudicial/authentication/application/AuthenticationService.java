package br.com.monitorjudicial.authentication.application;

import br.com.monitorjudicial.authentication.domain.RefreshToken;
import br.com.monitorjudicial.authentication.domain.User;
import br.com.monitorjudicial.authentication.domain.port.AccessTokenIssuer;
import br.com.monitorjudicial.authentication.domain.port.PasswordHasher;
import br.com.monitorjudicial.authentication.domain.port.RefreshTokenRepository;
import br.com.monitorjudicial.authentication.domain.port.UserRepository;
import br.com.monitorjudicial.shared.audit.Auditor;
import br.com.monitorjudicial.shared.domain.BusinessRuleViolationException;
import br.com.monitorjudicial.shared.domain.Email;
import br.com.monitorjudicial.shared.domain.Ids;
import br.com.monitorjudicial.shared.domain.UnauthorizedException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** UC01, UC02 e logout: emissão, rotação com detecção de reuso e revogação de tokens. */
@Service
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final UnauthorizedException INVALID_CREDENTIALS =
            new UnauthorizedException("invalid-credentials", "E-mail ou senha inválidos.");

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordHasher passwordHasher;
    private final AccessTokenIssuer accessTokenIssuer;
    private final Auditor auditor;
    private final Clock clock;
    private final Duration refreshValidity;

    public AuthenticationService(UserRepository users, RefreshTokenRepository refreshTokens,
            PasswordHasher passwordHasher, AccessTokenIssuer accessTokenIssuer, Auditor auditor, Clock clock,
            @Value("${security.jwt.refresh-token-validity:P7D}") Duration refreshValidity) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordHasher = passwordHasher;
        this.accessTokenIssuer = accessTokenIssuer;
        this.auditor = auditor;
        this.clock = clock;
        this.refreshValidity = refreshValidity;
    }

    @Transactional
    public TokenPair authenticate(String email, String password) {
        Email parsed;
        try {
            parsed = new Email(email);
        } catch (BusinessRuleViolationException e) {
            throw INVALID_CREDENTIALS;
        }
        User user = users.findByEmail(parsed)
                .filter(User::active)
                .filter(u -> passwordHasher.matches(password, u.passwordHash()))
                .orElseThrow(() -> INVALID_CREDENTIALS);
        TokenPair pair = issuePair(user, Ids.newId());
        auditor.record("LOGIN", "user", user.id(), Map.of());
        return pair;
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public TokenPair refresh(String rawRefreshToken) {
        Instant now = clock.instant();
        RefreshToken current = refreshTokens.findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(() -> new UnauthorizedException("invalid-refresh-token", "Refresh token inválido."));
        if (current.isRevoked()) {
            int revoked = refreshTokens.revokeFamily(current.familyId(), now);
            log.warn("Reuso de refresh token detectado; família {} revogada ({} tokens)", current.familyId(), revoked);
            auditor.record("REFRESH_TOKEN_REUSE", "user", current.userId(), Map.of("familyId", current.familyId()));
            throw new UnauthorizedException("refresh-token-reused",
                    "Refresh token já utilizado. Por segurança, todas as sessões desta cadeia foram encerradas.");
        }
        if (current.isExpired(now)) {
            throw new UnauthorizedException("refresh-token-expired", "Refresh token expirado.");
        }
        User user = users.findById(current.userId())
                .filter(User::active)
                .orElseThrow(() -> new UnauthorizedException("inactive-user", "Usuário inativo."));

        String raw = newRawToken();
        RefreshToken successor = RefreshToken.issue(user.id(), hash(raw), current.familyId(), now,
                now.plus(refreshValidity));
        current.rotateTo(successor.id(), now);
        refreshTokens.save(current);
        refreshTokens.save(successor);
        return TokenPair.bearer(accessTokenIssuer.issue(user), raw, accessTokenIssuer.validity().toSeconds());
    }

    @Transactional
    public void revoke(String rawRefreshToken) {
        refreshTokens.findByTokenHash(hash(rawRefreshToken)).ifPresent(token -> {
            refreshTokens.revokeFamily(token.familyId(), clock.instant());
            auditor.record("LOGOUT", "user", token.userId(), Map.of());
        });
    }

    private TokenPair issuePair(User user, UUID familyId) {
        Instant now = clock.instant();
        String raw = newRawToken();
        refreshTokens.save(RefreshToken.issue(user.id(), hash(raw), familyId, now, now.plus(refreshValidity)));
        return TokenPair.bearer(accessTokenIssuer.issue(user), raw, accessTokenIssuer.validity().toSeconds());
    }

    private static String newRawToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((raw == null ? "" : raw).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
