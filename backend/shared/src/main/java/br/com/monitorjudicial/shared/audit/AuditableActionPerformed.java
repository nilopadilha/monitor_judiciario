package br.com.monitorjudicial.shared.audit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Evento publicado por qualquer módulo após uma escrita; persistido pelo módulo de auditoria na mesma transação. */
public record AuditableActionPerformed(
        UUID actorId,
        String actorEmail,
        String action,
        String resourceType,
        String resourceId,
        Map<String, Object> details,
        String correlationId,
        Instant occurredAt) {

    public AuditableActionPerformed {
        details = details == null ? Map.of() : Map.copyOf(details);
    }
}
