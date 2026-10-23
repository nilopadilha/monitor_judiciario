package br.com.monitorjudicial.shared.domain;

/** Verificações de pré-condição para construtores do domínio. */
public final class Guard {

    private Guard() {
    }

    public static String requireText(String value, String code, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleViolationException(code, message);
        }
        return value.strip();
    }

    public static <T> T requireNonNull(T value, String code, String message) {
        if (value == null) {
            throw new BusinessRuleViolationException(code, message);
        }
        return value;
    }
}
