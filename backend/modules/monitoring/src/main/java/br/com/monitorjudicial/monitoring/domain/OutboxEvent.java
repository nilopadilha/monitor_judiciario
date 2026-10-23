package br.com.monitorjudicial.monitoring.domain;

import java.time.Instant;
import java.util.UUID;

/** Evento gravado na mesma transação do fato de negócio, publicado depois pelo relay (ADR-0004). */
public record OutboxEvent(
        UUID id,
        String aggregateType,
        UUID aggregateId,
        String eventType,
        String payload,
        String correlationId,
        Instant createdAt) {
}
