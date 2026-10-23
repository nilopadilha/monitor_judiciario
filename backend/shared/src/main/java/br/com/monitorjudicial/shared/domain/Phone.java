package br.com.monitorjudicial.shared.domain;

/**
 * Telefone em formato E.164. Números brasileiros sem DDI recebem +55.
 */
public record Phone(String value) {

    public Phone {
        String digits = Guard.requireText(value, "invalid-phone", "Telefone é obrigatório.").replaceAll("\\D", "");
        if (digits.length() == 10 || digits.length() == 11) {
            digits = "55" + digits;
        }
        if (digits.length() < 12 || digits.length() > 15) {
            throw new BusinessRuleViolationException("invalid-phone", "Telefone inválido: " + value);
        }
        value = "+" + digits;
    }

    @Override
    public String toString() {
        return value;
    }
}
