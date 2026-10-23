package br.com.monitorjudicial.authentication.infrastructure.security;

import jakarta.validation.constraints.NotBlank;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Configuração JWT. A aplicação não inicia com segredo menor que 256 bits (ADR-0009). */
@Validated
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(@NotBlank String secret, String issuer, Duration accessTokenValidity) {

    public JwtProperties {
        if (secret != null && secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("security.jwt.secret precisa ter ao menos 32 bytes (256 bits).");
        }
        issuer = issuer == null ? "monitor-judicial" : issuer;
        accessTokenValidity = accessTokenValidity == null ? Duration.ofMinutes(15) : accessTokenValidity;
    }
}
