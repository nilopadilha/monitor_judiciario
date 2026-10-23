package br.com.monitorjudicial.shared.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/** Endereço de e-mail normalizado em minúsculas. */
public record Email(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[^@\\s]{1,64}@[^@\\s]+\\.[^@\\s]{2,}$");

    public Email {
        String normalized = Guard.requireText(value, "invalid-email", "E-mail é obrigatório.").toLowerCase(Locale.ROOT);
        if (normalized.length() > 254 || !FORMAT.matcher(normalized).matches()) {
            throw new BusinessRuleViolationException("invalid-email", "E-mail inválido: " + value);
        }
        value = normalized;
    }

    @Override
    public String toString() {
        return value;
    }
}
