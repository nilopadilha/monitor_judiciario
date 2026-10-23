package br.com.monitorjudicial.clients.domain;

import br.com.monitorjudicial.shared.domain.BusinessRuleViolationException;
import br.com.monitorjudicial.shared.domain.Email;
import br.com.monitorjudicial.shared.domain.Guard;
import br.com.monitorjudicial.shared.domain.Ids;
import br.com.monitorjudicial.shared.domain.Phone;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Contratante do monitoramento. */
public final class Client {

    private final UUID id;
    private String name;
    private final Document document;
    private Email email;
    private Phone phone;
    private ClientStatus status;
    private boolean notifyEmail;
    private boolean notifyWhatsapp;
    private final Instant createdAt;
    private Instant updatedAt;

    public Client(UUID id, String name, Document document, Email email, Phone phone, ClientStatus status,
            boolean notifyEmail, boolean notifyWhatsapp, Instant createdAt, Instant updatedAt) {
        this.id = Guard.requireNonNull(id, "invalid-client", "Id é obrigatório.");
        this.document = Guard.requireNonNull(document, "invalid-document", "Documento é obrigatório.");
        this.status = Guard.requireNonNull(status, "invalid-client-status", "Status é obrigatório.");
        this.createdAt = createdAt;
        applyContact(name, email, phone, notifyEmail, notifyWhatsapp, updatedAt);
    }

    public static Client register(String name, Document document, Email email, Phone phone, boolean notifyEmail,
            boolean notifyWhatsapp, Instant now) {
        return new Client(Ids.newId(), name, document, email, phone, ClientStatus.ACTIVE, notifyEmail, notifyWhatsapp,
                now, now);
    }

    public void update(String name, Email email, Phone phone, boolean notifyEmail, boolean notifyWhatsapp,
            Instant now) {
        applyContact(name, email, phone, notifyEmail, notifyWhatsapp, now);
    }

    public void deactivate(Instant now) {
        this.status = ClientStatus.INACTIVE;
        this.updatedAt = now;
    }

    public void activate(Instant now) {
        this.status = ClientStatus.ACTIVE;
        this.updatedAt = now;
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

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Document document() {
        return document;
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

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
