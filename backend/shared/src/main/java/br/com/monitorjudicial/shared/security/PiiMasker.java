package br.com.monitorjudicial.shared.security;

/** Utilitário de mascaramento de dados pessoais (LGPD) para logs, auditoria e listagens. */
public final class PiiMasker {

    private PiiMasker() {
    }

    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "***";
        }
        String trimmed = email.strip();
        int at = trimmed.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        String local = trimmed.substring(0, at);
        String domain = trimmed.substring(at);
        return local.charAt(0) + "***" + domain;
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return "***";
        }
        String trimmed = phone.strip();
        if (trimmed.length() <= 4) {
            return "****";
        }
        String prefix = trimmed.length() > 8 ? trimmed.substring(0, 5) : "";
        String suffix = trimmed.substring(trimmed.length() - 4);
        return prefix + "*****" + suffix;
    }
}
