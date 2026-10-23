package br.com.monitorjudicial.audit.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Registro imutável de uma ação. */
public record AuditLog(UUID id, UUID actorId, String actorEmail, String action, String resourceType,
        String resourceId, Map<String, Object> details, String correlationId, Instant occurredAt) {
}
