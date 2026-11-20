package br.com.monitorjudicial.lawyers.domain;

import br.com.monitorjudicial.shared.domain.BusinessRuleViolationException;
import br.com.monitorjudicial.shared.domain.Email;
import br.com.monitorjudicial.shared.domain.Guard;
import br.com.monitorjudicial.shared.domain.Ids;
import br.com.monitorjudicial.shared.domain.Phone;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Advogado do escritório responsável por carteiras, áreas do direito e monitoramento por OAB. */
public final class Lawyer {

    private final UUID id;
    private String name;
    private final OabNumber oab;
    private PracticeArea practiceArea;
    private Email email;
    private Phone phone;
    private UUID userId;
    private boolean notifyEmail;
    private boolean notifyWhatsapp;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;

    public Lawyer(UUID id, String name, OabNumber oab, PracticeArea practiceArea, Email email, Phone phone,
            UUID userId, boolean notifyEmail, boolean notifyWhatsapp, boolean active, Instant createdAt,
            Instant updatedAt) {
        this.id = Guard.requireNonNull(id, "invalid-lawyer", "Id é obrigatório.");
        this.oab = Guard.requireNonNull(oab, "invalid-oab", "OAB é obrigatória.");
        this.active = active;
        this.createdAt = createdAt;
        applyProfile(name, practiceArea, email, phone, userId, notifyEmail, notifyWhatsapp, updatedAt);
    }

    public static Lawyer register(String name, OabNumber oab, PracticeArea practiceArea, Email email, Phone phone,
            UUID userId, boolean notifyEmail, boolean notifyWhatsapp, Instant now) {
        return new Lawyer(Ids.newId(), name, oab, practiceArea, email, phone, userId, notifyEmail, notifyWhatsapp,
                true, now, now);
    }

    public void update(String name, PracticeArea practiceArea, Email email, Phone phone, UUID userId,
            boolean notifyEmail, boolean notifyWhatsapp, Instant now) {
        applyProfile(name, practiceArea, email, phone, userId, notifyEmail, notifyWhatsapp, now);
    }

    public void setActive(boolean nextActive, Instant now) {
        this.active = nextActive;
        this.updatedAt = now;
    }

    private void applyProfile(String name, PracticeArea practiceArea, Email email, Phone phone, UUID userId,
            boolean notifyEmail, boolean notifyWhatsapp, Instant now) {
        String trimmed = Guard.requireText(name, "invalid-lawyer-name", "Nome do advogado é obrigatório.");
        if (trimmed.length() > 200) {
            throw new BusinessRuleViolationException("invalid-lawyer-name", "Nome deve ter até 200 caracteres.");
        }
        Guard.requireNonNull(email, "invalid-email", "E-mail é obrigatório.");
        if (notifyWhatsapp && phone == null) {
            throw new BusinessRuleViolationException("whatsapp-requires-phone",
                    "Para notificar por WhatsApp, informe o telefone.");
        }
        this.name = trimmed;
        this.practiceArea = practiceArea == null ? PracticeArea.CIVEL : practiceArea;
        this.email = email;
        this.phone = phone;
        this.userId = userId;
        this.notifyEmail = notifyEmail;
        this.notifyWhatsapp = notifyWhatsapp;
        this.updatedAt = now;
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public OabNumber oab() {
        return oab;
    }

    public PracticeArea practiceArea() {
        return practiceArea;
    }

    public Email email() {
        return email;
    }

    public Optional<Phone> phone() {
        return Optional.ofNullable(phone);
    }

    public Optional<UUID> userId() {
        return Optional.ofNullable(userId);
    }

    public boolean notifyEmail() {
        return notifyEmail;
    }

    public boolean notifyWhatsapp() {
        return notifyWhatsapp;
    }

    public boolean active() {
        return active;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
