package br.com.monitorjudicial.monitoring.domain;

import br.com.monitorjudicial.shared.domain.Ids;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Verificação de um processo dentro de uma execução. */
public record MonitoringJob(
        UUID id,
        UUID executionId,
        UUID processId,
        LocalDate executionDate,
        JobStatus status,
        long executionTimeMs,
        int newPublications,
        String errorMessage,
        Instant createdAt) {

    private static final int MAX_ERROR = 500;

    public static MonitoringJob success(UUID executionId, UUID processId, LocalDate date, long elapsedMs,
            int newPublications, Instant now) {
        return new MonitoringJob(Ids.newId(), executionId, processId, date, JobStatus.SUCCESS, elapsedMs,
                newPublications, null, now);
    }

    public static MonitoringJob failure(UUID executionId, UUID processId, LocalDate date, long elapsedMs,
            String error, Instant now) {
        String message = error == null ? "Erro desconhecido" : error;
        return new MonitoringJob(Ids.newId(), executionId, processId, date, JobStatus.FAILED, elapsedMs, 0,
                message.length() > MAX_ERROR ? message.substring(0, MAX_ERROR) : message, now);
    }
}
