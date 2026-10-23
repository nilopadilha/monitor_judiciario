package br.com.monitorjudicial.monitoring.application;

import br.com.monitorjudicial.monitoring.domain.ExecutionStatus;
import br.com.monitorjudicial.monitoring.domain.ExecutionTrigger;
import br.com.monitorjudicial.monitoring.domain.MonitoringExecution;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MonitoringExecutionView(
        UUID id,
        ExecutionTrigger trigger,
        String triggeredBy,
        ExecutionStatus status,
        Instant startedAt,
        Instant finishedAt,
        int processesChecked,
        int newPublications,
        int failedJobs,
        String errorMessage,
        List<JobView> jobs) {

    public static MonitoringExecutionView of(MonitoringExecution e, List<JobView> jobs) {
        return new MonitoringExecutionView(e.id(), e.trigger(), e.triggeredBy(), e.status(), e.startedAt(),
                e.finishedAt().orElse(null), e.processesChecked(), e.newPublications(), e.failedJobs(),
                e.errorMessage().orElse(null), jobs);
    }

    public record JobView(UUID processId, String processNumber, String status, long executionTimeMs,
            int newPublications, String errorMessage) {
    }
}
