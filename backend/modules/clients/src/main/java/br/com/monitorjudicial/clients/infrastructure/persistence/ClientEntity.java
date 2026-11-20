package br.com.monitorjudicial.clients.infrastructure.persistence;

import br.com.monitorjudicial.clients.domain.ClientStatus;
import br.com.monitorjudicial.clients.domain.LegalBasis;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "clients")
@Getter
@Setter
@NoArgsConstructor
public class ClientEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String document;

    @Column(nullable = false)
    private String email;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClientStatus status;

    @Column(name = "notify_email", nullable = false)
    private boolean notifyEmail;

    @Column(name = "notify_whatsapp", nullable = false)
    private boolean notifyWhatsapp;

    @Enumerated(EnumType.STRING)
    @Column(name = "legal_basis", nullable = false)
    private LegalBasis legalBasis = LegalBasis.CONTRACT_EXECUTION;

    @Column(name = "privacy_notice_version")
    private String privacyNoticeVersion;

    @Column(name = "consent_recorded_at")
    private Instant consentRecordedAt;

    @Column(name = "anonymized_at")
    private Instant anonymizedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
