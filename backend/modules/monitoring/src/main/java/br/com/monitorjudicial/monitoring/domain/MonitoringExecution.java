package br.com.monitorjudicial.monitoring.domain;

import br.com.monitorjudicial.shared.domain.Ids;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Uma rodada de monitoramento (agendada ou manual), agrupando um job por processo (Q4). */
public final class MonitoringExecution {

    private final UUID id;
    private final ExecutionTrigger trigger;
    private final String triggeredBy;
    private final Instant startedAt;
    private ExecutionStatus status;
    private Instant finishedAt;
    private int processesChecked;
    private int newPublications;
    private int failedJobs;
    private String errorMessage;

    public MonitoringExecution(UUID id, ExecutionTrigger trigger, String triggeredBy, Instant startedAt,
            ExecutionStatus status, Instant finishedAt, int processesChecked, int newPublications, int failedJobs,
            String errorMessage) {
        this.id = id;
        this.trigger = trigger;
        this.triggeredBy = triggeredBy;
        this.startedAt = startedAt;
        this.status = status;
        this.finishedAt = finishedAt;
        this.processesChecked = processesChecked;
        this.newPublications = newPublications;
        this.failedJobs = failedJobs;
        this.errorMessage = errorMessage;
    }

    public static MonitoringExecution start(ExecutionTrigger trigger, String triggeredBy, Instant now) {
        return new MonitoringExecution(Ids.newId(), trigger, triggeredBy, now, ExecutionStatus.RUNNING, null, 0, 0, 0,
                null);
    }

    public void complete(int processesChecked, int newPublications, int failedJobs, Instant now) {
        this.processesChecked = processesChecked;
        this.newPublications = newPublications;
        this.failedJobs = failedJobs;
        this.status = failedJobs == 0 ? ExecutionStatus.COMPLETED : ExecutionStatus.COMPLETED_WITH_FAILURES;
        this.finishedAt = now;
    }

    public void fail(String message, Instant now) {
        this.status = ExecutionStatus.FAILED;
        this.errorMessage = message;
        this.finishedAt = now;
    }

    public UUID id() {
        return id;
    }

    public ExecutionTrigger trigger() {
        return trigger;
    }

    public String triggeredBy() {
        return triggeredBy;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public ExecutionStatus status() {
        return status;
    }

    public Optional<Instant> finishedAt() {
        return Optional.ofNullable(finishedAt);
    }

    public int processesChecked() {
        return processesChecked;
    }

    public int newPublications() {
        return newPublications;
    }

    public int failedJobs() {
        return failedJobs;
    }

    public Optional<String> errorMessage() {
        return Optional.ofNullable(errorMessage);
    }
}
