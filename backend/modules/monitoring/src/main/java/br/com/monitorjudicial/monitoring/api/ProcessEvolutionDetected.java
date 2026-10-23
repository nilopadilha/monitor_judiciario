package br.com.monitorjudicial.monitoring.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Evento de integração publicado via outbox quando uma publicação nova é detectada. */
public record ProcessEvolutionDetected(
        UUID eventId,
        UUID processId,
        UUID clientId,
        String processNumber,
        String courtCode,
        UUID publicationId,
        LocalDate publicationDate,
        String movementType,
        String movementLabel,
        String excerpt,
        Instant detectedAt) {
}
