package br.com.monitorjudicial.clients.application;

import br.com.monitorjudicial.clients.domain.LegalBasis;

/** Dados de cadastro/alteração. O documento é ignorado na alteração (imutável). */
public record ClientCommand(String name, String document, String email, String phone, boolean notifyEmail,
        boolean notifyWhatsapp, LegalBasis legalBasis, String privacyNoticeVersion) {

    public ClientCommand(String name, String document, String email, String phone, boolean notifyEmail,
            boolean notifyWhatsapp) {
        this(name, document, email, phone, notifyEmail, notifyWhatsapp, LegalBasis.CONTRACT_EXECUTION, null);
    }
}
