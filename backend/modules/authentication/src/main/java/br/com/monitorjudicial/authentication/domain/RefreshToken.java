package br.com.monitorjudicial.authentication.domain;

import br.com.monitorjudicial.shared.domain.Ids;
import java.time.Instant;
import java.util.UUID;

/**
 * Refresh token persistido apenas como hash. Tokens de uma mesma cadeia de rotação compartilham {@code familyId}.
 */
public final class RefreshToken {

    private final UUID id;
    private final UUID userId;
    private final String tokenHash;
    private final UUID familyId;
    private final Instant expiresAt;
    private final Instant createdAt;
    private Instant revokedAt;
    private UUID replacedById;

    public RefreshToken(UUID id, UUID userId, String tokenHash, UUID familyId, Instant expiresAt, Instant createdAt,
            Instant revokedAt, UUID replacedById) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.familyId = familyId;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.revokedAt = revokedAt;
        this.replacedById = replacedById;
    }

    public static RefreshToken issue(UUID userId, String tokenHash, UUID familyId, Instant now, Instant expiresAt) {
        return new RefreshToken(Ids.newId(), userId, tokenHash, familyId, expiresAt, now, null, null);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public void rotateTo(UUID successorId, Instant now) {
        this.revokedAt = now;
        this.replacedById = successorId;
    }

    public void revoke(Instant now) {
        if (revokedAt == null) {
            revokedAt = now;
        }
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public String tokenHash() {
        return tokenHash;
    }

    public UUID familyId() {
        return familyId;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant revokedAt() {
        return revokedAt;
    }

    public UUID replacedById() {
        return replacedById;
    }
}
