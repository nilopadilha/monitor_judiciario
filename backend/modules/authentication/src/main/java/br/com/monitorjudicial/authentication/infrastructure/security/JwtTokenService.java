package br.com.monitorjudicial.authentication.infrastructure.security;

import br.com.monitorjudicial.authentication.domain.User;
import br.com.monitorjudicial.authentication.domain.port.AccessTokenIssuer;
import br.com.monitorjudicial.shared.security.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/** Emite e valida access tokens JWT HS256. */
@Component
public class JwtTokenService implements AccessTokenIssuer {

    private final JwtProperties properties;
    private final SecretKey key;
    private final Clock clock;

    public JwtTokenService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.clock = clock;
    }

    @Override
    public String issue(User user) {
        Instant now = clock.instant();
        var builder = Jwts.builder()
                .issuer(properties.issuer())
                .subject(user.id().toString())
                .claim("email", user.email().value())
                .claim("role", user.role().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.accessTokenValidity())));
        user.clientId().ifPresent(id -> builder.claim("clientId", id.toString()));
        return builder.signWith(key, Jwts.SIG.HS256).compact();
    }

    @Override
    public Duration validity() {
        return properties.accessTokenValidity();
    }

    /** Valida assinatura, emissor e expiração; vazio para qualquer token inválido. */
    public Optional<AuthenticatedUser> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(properties.issuer())
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String clientId = claims.get("clientId", String.class);
            return Optional.of(new AuthenticatedUser(
                    UUID.fromString(claims.getSubject()),
                    claims.get("email", String.class),
                    Role.valueOf(claims.get("role", String.class)),
                    Optional.ofNullable(clientId).map(UUID::fromString)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
