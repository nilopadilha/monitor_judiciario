package br.com.monitorjudicial.monitoring.infrastructure.persistence;

import br.com.monitorjudicial.monitoring.domain.ExecutionStatus;
import br.com.monitorjudicial.monitoring.domain.ExecutionTrigger;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "monitoring_executions")
@Getter
@Setter
@NoArgsConstructor
public class MonitoringExecutionEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false)
    private ExecutionTrigger trigger;

    @Column(name = "triggered_by", nullable = false)
    private String triggeredBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "processes_checked", nullable = false)
    private int processesChecked;

    @Column(name = "new_publications", nullable = false)
    private int newPublications;

    @Column(name = "failed_jobs", nullable = false)
    private int failedJobs;

    @Column(name = "error_message")
    private String errorMessage;
}
