package br.com.monitorjudicial.shared.observability;

import java.util.Optional;
import org.slf4j.MDC;

/** Acesso ao identificador de correlação mantido no MDC. */
public final class CorrelationId {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    private CorrelationId() {
    }

    public static Optional<String> current() {
        return Optional.ofNullable(MDC.get(MDC_KEY));
    }
}
