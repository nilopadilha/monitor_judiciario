package br.com.monitorjudicial.authentication.domain;

import br.com.monitorjudicial.shared.domain.BusinessRuleViolationException;
import br.com.monitorjudicial.shared.domain.Email;
import br.com.monitorjudicial.shared.domain.Guard;
import br.com.monitorjudicial.shared.domain.Ids;
import br.com.monitorjudicial.shared.security.Role;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Usuário do sistema. Um CUSTOMER é sempre vinculado a exatamente um cliente. */
public final class User {

    private final UUID id;
    private final String name;
    private final Email email;
    private final String passwordHash;
    private final Role role;
    private final UUID clientId;
    private final boolean active;
    private final Instant createdAt;
    private final Instant updatedAt;

    public User(UUID id, String name, Email email, String passwordHash, Role role, UUID clientId, boolean active,
            Instant createdAt, Instant updatedAt) {
        this.id = Guard.requireNonNull(id, "invalid-user", "Id é obrigatório.");
        this.name = Guard.requireText(name, "invalid-user-name", "Nome é obrigatório.");
        this.email = Guard.requireNonNull(email, "invalid-email", "E-mail é obrigatório.");
        this.passwordHash = Guard.requireText(passwordHash, "invalid-password", "Senha é obrigatória.");
        this.role = Guard.requireNonNull(role, "invalid-role", "Perfil é obrigatório.");
        if (role == Role.CUSTOMER && clientId == null) {
            throw new BusinessRuleViolationException("customer-requires-client",
                    "Usuário CUSTOMER precisa estar vinculado a um cliente.");
        }
        if (role != Role.CUSTOMER && clientId != null) {
            throw new BusinessRuleViolationException("client-only-for-customer",
                    "Apenas usuários CUSTOMER são vinculados a um cliente.");
        }
        this.clientId = clientId;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static User create(String name, Email email, String passwordHash, Role role, UUID clientId, Instant now) {
        return new User(Ids.newId(), name, email, passwordHash, role, clientId, true, now, now);
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Email email() {
        return email;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public Role role() {
        return role;
    }

    public Optional<UUID> clientId() {
        return Optional.ofNullable(clientId);
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
