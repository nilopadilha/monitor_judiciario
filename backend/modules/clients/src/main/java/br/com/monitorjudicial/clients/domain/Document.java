package br.com.monitorjudicial.clients.domain;

import br.com.monitorjudicial.shared.domain.BusinessRuleViolationException;
import br.com.monitorjudicial.shared.domain.Guard;

/** CPF (11 dígitos) ou CNPJ (14 dígitos), armazenado só com dígitos e validado pelos dígitos verificadores. */
public record Document(String value) {

    public Document {
        String digits = Guard.requireText(value, "invalid-document", "Documento é obrigatório.").replaceAll("\\D", "");
        boolean valid = switch (digits.length()) {
            case 11 -> isValidCpf(digits);
            case 14 -> isValidCnpj(digits);
            default -> false;
        };
        if (!valid) {
            throw new BusinessRuleViolationException("invalid-document", "CPF/CNPJ inválido: " + value);
        }
        value = digits;
    }

    public boolean isCpf() {
        return value.length() == 11;
    }

    /** Documento mascarado para logs e auditoria (LGPD). */
    public String masked() {
        return isCpf()
                ? "***." + value.substring(3, 6) + ".***-**"
                : value.substring(0, 2) + ".***.***/" + value.substring(8, 12) + "-**";
    }

    public String formatted() {
        return isCpf()
                ? value.replaceFirst("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4")
                : value.replaceFirst("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
    }

    private static boolean isValidCpf(String d) {
        if (d.chars().distinct().count() == 1) {
            return false;
        }
        return checkDigit(d, 9, 10) == d.charAt(9) - '0' && checkDigit(d, 10, 11) == d.charAt(10) - '0';
    }

    private static int checkDigit(String d, int length, int startWeight) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += (d.charAt(i) - '0') * (startWeight - i);
        }
        int rest = (sum * 10) % 11;
        return rest == 10 ? 0 : rest;
    }

    private static boolean isValidCnpj(String d) {
        if (d.chars().distinct().count() == 1) {
            return false;
        }
        int[] w1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] w2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        return cnpjDigit(d, w1) == d.charAt(12) - '0' && cnpjDigit(d, w2) == d.charAt(13) - '0';
    }

    private static int cnpjDigit(String d, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += (d.charAt(i) - '0') * weights[i];
        }
        int rest = sum % 11;
        return rest < 2 ? 0 : 11 - rest;
    }
}
