package br.com.monitorjudicial.clients.domain;

import br.com.monitorjudicial.shared.domain.BusinessRuleViolationException;
import br.com.monitorjudicial.shared.domain.Email;
import br.com.monitorjudicial.shared.domain.Guard;
import br.com.monitorjudicial.shared.domain.Ids;
import br.com.monitorjudicial.shared.domain.Phone;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Contratante do monitoramento, com suporte a base legal e anonimização LGPD. */
public final class Client {

    private static final String ANONYMIZED_NAME = "Titular anonimizado";
    private static final String ANONYMIZED_DOCUMENT_LABEL = "[anonimizado]";

    private final UUID id;
    private String name;
    private Document document;
    private String storedDocument;
    private Email email;
    private Phone phone;
    private ClientStatus status;
    private boolean notifyEmail;
    private boolean notifyWhatsapp;
    private LegalBasis legalBasis;
    private String privacyNoticeVersion;
    private Instant consentRecordedAt;
    private Instant anonymizedAt;
    private final Instant createdAt;
    private Instant updatedAt;

    public Client(UUID id, String name, Document document, Email email, Phone phone, ClientStatus status,
            boolean notifyEmail, boolean notifyWhatsapp, Instant createdAt, Instant updatedAt) {
        this(id, name, document, document == null ? null : document.value(), email, phone, status, notifyEmail,
                notifyWhatsapp, LegalBasis.CONTRACT_EXECUTION, null, null, null, createdAt, updatedAt);
    }

    public Client(UUID id, String name, Document document, String storedDocument, Email email, Phone phone,
            ClientStatus status, boolean notifyEmail, boolean notifyWhatsapp, LegalBasis legalBasis,
            String privacyNoticeVersion, Instant consentRecordedAt, Instant anonymizedAt, Instant createdAt,
            Instant updatedAt) {
        this.id = Guard.requireNonNull(id, "invalid-client", "Id é obrigatório.");
        this.status = Guard.requireNonNull(status, "invalid-client-status", "Status é obrigatório.");
        this.legalBasis = legalBasis == null ? LegalBasis.CONTRACT_EXECUTION : legalBasis;
        this.privacyNoticeVersion = normalizeVersion(privacyNoticeVersion);
        this.consentRecordedAt = consentRecordedAt;
        this.anonymizedAt = anonymizedAt;
        this.createdAt = createdAt;
        if (status == ClientStatus.ANONYMIZED) {
            this.document = null;
            this.storedDocument = Guard.requireText(storedDocument, "invalid-document", "Documento é obrigatório.");
            this.name = Guard.requireText(name, "invalid-client-name", "Nome é obrigatório.");
            this.email = Guard.requireNonNull(email, "invalid-email", "E-mail é obrigatório.");
            this.phone = null;
            this.notifyEmail = false;
            this.notifyWhatsapp = false;
            this.updatedAt = updatedAt;
        } else {
            this.document = Guard.requireNonNull(document, "invalid-document", "Documento é obrigatório.");
            this.storedDocument = document.value();
            applyContact(name, email, phone, notifyEmail, notifyWhatsapp, updatedAt);
        }
    }

    public static Client register(String name, Document document, Email email, Phone phone, boolean notifyEmail,
            boolean notifyWhatsapp, Instant now) {
        return register(name, document, email, phone, notifyEmail, notifyWhatsapp, LegalBasis.CONTRACT_EXECUTION,
                "1.0", now);
    }

    public static Client register(String name, Document document, Email email, Phone phone, boolean notifyEmail,
            boolean notifyWhatsapp, LegalBasis legalBasis, String privacyNoticeVersion, Instant now) {
        LegalBasis basis = legalBasis == null ? LegalBasis.CONTRACT_EXECUTION : legalBasis;
        String version = normalizeVersion(privacyNoticeVersion);
        return new Client(Ids.newId(), name, document, document.value(), email, phone, ClientStatus.ACTIVE,
                notifyEmail, notifyWhatsapp, basis, version == null ? "1.0" : version, now, null, now, now);
    }

    public void update(String name, Email email, Phone phone, boolean notifyEmail, boolean notifyWhatsapp,
            Instant now) {
        ensureNotAnonymized();
        applyContact(name, email, phone, notifyEmail, notifyWhatsapp, now);
    }

    public void update(String name, Email email, Phone phone, boolean notifyEmail, boolean notifyWhatsapp,
            LegalBasis legalBasis, String privacyNoticeVersion, Instant now) {
        ensureNotAnonymized();
        if (legalBasis != null) {
            this.legalBasis = legalBasis;
        }
        String version = normalizeVersion(privacyNoticeVersion);
        if (version != null) {
            this.privacyNoticeVersion = version;
            this.consentRecordedAt = now;
        }
        applyContact(name, email, phone, notifyEmail, notifyWhatsapp, now);
    }

    public void updateNotificationPreferences(boolean notifyEmail, boolean notifyWhatsapp, Instant now) {
        ensureNotAnonymized();
        applyContact(this.name, this.email, this.phone, notifyEmail, notifyWhatsapp, now);
    }

    public void deactivate(Instant now) {
        ensureNotAnonymized();
        this.status = ClientStatus.INACTIVE;
        this.updatedAt = now;
    }

    public void activate(Instant now) {
        ensureNotAnonymized();
        this.status = ClientStatus.ACTIVE;
        this.updatedAt = now;
    }

    public void anonymize(Instant now) {
        if (status == ClientStatus.ANONYMIZED) {
            throw new BusinessRuleViolationException("client-already-anonymized",
                    "Os dados deste titular já foram anonimizados.");
        }
        this.name = ANONYMIZED_NAME;
        this.document = null;
        this.storedDocument = "ANON-" + id;
        this.email = new Email("anon-" + id + "@anonimizado.invalid");
        this.phone = null;
        this.notifyEmail = false;
        this.notifyWhatsapp = false;
        this.status = ClientStatus.ANONYMIZED;
        this.anonymizedAt = now;
        this.updatedAt = now;
    }

    private void ensureNotAnonymized() {
        if (status == ClientStatus.ANONYMIZED) {
            throw new BusinessRuleViolationException("client-anonymized",
                    "Titular anonimizado não pode ser alterado.");
        }
    }

    private static String normalizeVersion(String version) {
        if (version == null || version.isBlank()) {
            return null;
        }
        String trimmed = version.strip();
        return trimmed.length() > 32 ? trimmed.substring(0, 32) : trimmed;
    }

    private void applyContact(String name, Email email, Phone phone, boolean notifyEmail, boolean notifyWhatsapp,
            Instant now) {
        String trimmed = Guard.requireText(name, "invalid-client-name", "Nome é obrigatório.");
        if (trimmed.length() > 200) {
            throw new BusinessRuleViolationException("invalid-client-name", "Nome deve ter até 200 caracteres.");
        }
        Guard.requireNonNull(email, "invalid-email", "E-mail é obrigatório.");
        if (notifyWhatsapp && phone == null) {
            throw new BusinessRuleViolationException("whatsapp-requires-phone",
                    "Para notificar por WhatsApp, informe o telefone.");
        }
        if (!notifyEmail && !notifyWhatsapp) {
            throw new BusinessRuleViolationException("no-notification-channel",
                    "Habilite ao menos um canal de notificação.");
        }
        this.name = trimmed;
        this.email = email;
        this.phone = phone;
        this.notifyEmail = notifyEmail;
        this.notifyWhatsapp = notifyWhatsapp;
        this.updatedAt = now;
    }

    public boolean isActive() {
        return status == ClientStatus.ACTIVE;
    }

    public boolean isAnonymized() {
        return status == ClientStatus.ANONYMIZED;
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Document document() {
        return document;
    }

    public String storedDocument() {
        return storedDocument;
    }

    public String formattedDocument() {
        return document == null ? ANONYMIZED_DOCUMENT_LABEL : document.formatted();
    }

    public String maskedDocument() {
        return document == null ? ANONYMIZED_DOCUMENT_LABEL : document.masked();
    }

    public Email email() {
        return email;
    }

    public Optional<Phone> phone() {
        return Optional.ofNullable(phone);
    }

    public ClientStatus status() {
        return status;
    }

    public boolean notifyEmail() {
        return notifyEmail;
    }

    public boolean notifyWhatsapp() {
        return notifyWhatsapp;
    }

    public LegalBasis legalBasis() {
        return legalBasis;
    }

    public String privacyNoticeVersion() {
        return privacyNoticeVersion;
    }

    public Instant consentRecordedAt() {
        return consentRecordedAt;
    }

    public Instant anonymizedAt() {
        return anonymizedAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
