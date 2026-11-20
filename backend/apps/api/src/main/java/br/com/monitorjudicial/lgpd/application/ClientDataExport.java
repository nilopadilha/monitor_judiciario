package br.com.monitorjudicial.lgpd.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Pacote de portabilidade de dados pessoais do titular (Art. 18, V LGPD). */
public record ClientDataExport(
        Instant exportedAt,
        ClientProfile client,
        List<ProcessRecord> processes,
        List<NotificationRecord> notifications) {

    public record ClientProfile(UUID id, String name, String document, String email, String phone, String status,
            boolean notifyEmail, boolean notifyWhatsapp, String legalBasis, String privacyNoticeVersion,
            Instant consentRecordedAt, Instant anonymizedAt, Instant createdAt, Instant updatedAt) {
    }

    public record ProcessRecord(UUID id, String number, String courtCode, String subject, String status,
            boolean confidential, LocalDate lastPublicationDate, Instant createdAt) {
    }

    public record NotificationRecord(UUID id, UUID processId, String channel, String status, Instant sentAt,
            Instant createdAt) {
    }
}
