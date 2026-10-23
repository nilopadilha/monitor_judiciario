package br.com.monitorjudicial.monitoring.infrastructure.persistence;

import br.com.monitorjudicial.monitoring.domain.ExecutionStatus;
import br.com.monitorjudicial.monitoring.domain.MonitoringExecution;
import br.com.monitorjudicial.monitoring.domain.port.MonitoringExecutionRepository;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
class MonitoringExecutionPersistenceAdapter implements MonitoringExecutionRepository {

    private final MonitoringExecutionJpaRepository jpa;

    @Override
    @Transactional
    public MonitoringExecution save(MonitoringExecution x) {
        MonitoringExecutionEntity e = new MonitoringExecutionEntity();
        e.setId(x.id());
        e.setTrigger(x.trigger());
        e.setTriggeredBy(x.triggeredBy());
        e.setStatus(x.status());
        e.setStartedAt(x.startedAt());
        e.setFinishedAt(x.finishedAt().orElse(null));
        e.setProcessesChecked(x.processesChecked());
        e.setNewPublications(x.newPublications());
        e.setFailedJobs(x.failedJobs());
        e.setErrorMessage(x.errorMessage().map(m -> m.length() > 500 ? m.substring(0, 500) : m).orElse(null));
        jpa.save(e);
        return x;
    }

    @Override
    public Optional<MonitoringExecution> findById(UUID id) {
        return jpa.findById(id).map(MonitoringExecutionPersistenceAdapter::toDomain);
    }

    @Override
    public PageResult<MonitoringExecution> findRecent(PageQuery page) {
        var result = jpa.findAllByOrderByStartedAtDesc(PageRequest.of(page.page(), page.size()));
        return PageResult.of(result.map(MonitoringExecutionPersistenceAdapter::toDomain).getContent(), page.page(),
                page.size(), result.getTotalElements());
    }

    @Override
    @Transactional
    public int abortRunning(Instant now) {
        return jpa.abortRunning(ExecutionStatus.RUNNING, ExecutionStatus.ABORTED, now);
    }

    private static MonitoringExecution toDomain(MonitoringExecutionEntity e) {
        return new MonitoringExecution(e.getId(), e.getTrigger(), e.getTriggeredBy(), e.getStartedAt(), e.getStatus(),
                e.getFinishedAt(), e.getProcessesChecked(), e.getNewPublications(), e.getFailedJobs(),
                e.getErrorMessage());
    }
}
