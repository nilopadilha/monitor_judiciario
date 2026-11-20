package br.com.monitorjudicial.djen.infrastructure;

import br.com.monitorjudicial.shared.observability.CorrelationId;
import feign.Request;
import feign.RequestInterceptor;
import feign.RetryableException;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;

/**
 * Configuração do cliente DJEN (não é {@code @Configuration} para não vazar para outros Feign clients).
 * 429 e 5xx viram {@link RetryableException}; {@code Retry-After} é respeitado com teto de segurança.
 */
class DjenFeignConfiguration {

    private static final long MAX_RETRY_AFTER_SECONDS = 60L;

    @Bean
    Request.Options djenRequestOptions(DjenProperties properties) {
        return new Request.Options(properties.connectTimeout().toMillis(), TimeUnit.MILLISECONDS,
                properties.readTimeout().toMillis(), TimeUnit.MILLISECONDS, true);
    }

    @Bean
    Retryer djenRetryer(DjenProperties properties) {
        return new Retryer.Default(500, 5_000, properties.maxAttempts());
    }

    @Bean
    RequestInterceptor djenHeaders() {
        return template -> {
            template.header("Accept", "application/json");
            template.header("User-Agent", "MonitorJudicial/0.1 (+https://monitorjudicial.app)");
            CorrelationId.current().ifPresent(id -> template.header(CorrelationId.HEADER, id));
        };
    }

    @Bean
    ErrorDecoder djenErrorDecoder() {
        ErrorDecoder fallback = new ErrorDecoder.Default();
        return (methodKey, response) -> {
            int status = response.status();
            if (status == 429 || status >= 500) {
                return new RetryableException(status, "DJEN respondeu " + status, response.request().httpMethod(),
                        retryAfter(response.headers().get("Retry-After")), response.request());
            }
            return fallback.decode(methodKey, response);
        };
    }

    static Long retryAfter(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        String raw = values.iterator().next();
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        Instant now = Instant.now();
        try {
            long seconds = Math.clamp(Long.parseLong(trimmed), 0L, MAX_RETRY_AFTER_SECONDS);
            return now.plusSeconds(seconds).toEpochMilli();
        } catch (NumberFormatException ignored) {
            return parseHttpDate(trimmed, now);
        }
    }

    private static Long parseHttpDate(String value, Instant now) {
        try {
            Instant target = ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
            long seconds = Math.clamp(Duration.between(now, target).getSeconds(), 0L, MAX_RETRY_AFTER_SECONDS);
            return now.plusSeconds(seconds).toEpochMilli();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}
