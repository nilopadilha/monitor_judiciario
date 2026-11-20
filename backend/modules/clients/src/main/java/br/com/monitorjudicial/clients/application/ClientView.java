package br.com.monitorjudicial.clients.application;

import br.com.monitorjudicial.clients.domain.Client;
import br.com.monitorjudicial.clients.domain.ClientStatus;
import br.com.monitorjudicial.clients.domain.LegalBasis;
import br.com.monitorjudicial.shared.domain.Phone;
import java.time.Instant;
import java.util.UUID;

public record ClientView(UUID id, String name, String document, String documentMasked, String email, String phone,
        ClientStatus status, boolean notifyEmail, boolean notifyWhatsapp, LegalBasis legalBasis,
        String privacyNoticeVersion, Instant consentRecordedAt, Instant anonymizedAt, Instant createdAt,
        Instant updatedAt) {

    public static ClientView of(Client c) {
        return new ClientView(c.id(), c.name(), c.formattedDocument(), c.maskedDocument(), c.email().value(),
                c.phone().map(Phone::value).orElse(null), c.status(), c.notifyEmail(), c.notifyWhatsapp(),
                c.legalBasis(), c.privacyNoticeVersion(), c.consentRecordedAt(), c.anonymizedAt(),
                c.createdAt(), c.updatedAt());
    }

    /** Visão minimizada para listagens (Art. 6º, III LGPD): expõe somente documento mascarado. */
    public static ClientView masked(Client c) {
        return new ClientView(c.id(), c.name(), c.maskedDocument(), c.maskedDocument(), c.email().value(),
                c.phone().map(Phone::value).orElse(null), c.status(), c.notifyEmail(), c.notifyWhatsapp(),
                c.legalBasis(), c.privacyNoticeVersion(), c.consentRecordedAt(), c.anonymizedAt(),
                c.createdAt(), c.updatedAt());
    }
}
