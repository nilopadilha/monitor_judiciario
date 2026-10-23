package br.com.monitorjudicial.processes.application;

import br.com.monitorjudicial.processes.domain.ProcessStatus;
import java.time.Instant;
import java.util.UUID;

public record ProcessView(
        UUID id,
        UUID clientId,
        String clientName,
        String number,
        String courtCode,
        String subject,
        ProcessStatus status,
        MonitoringSituation situation,
        long unreadPublications,
        String lastError,
        Instant lastCheckedAt,
        Instant createdAt) {
}
