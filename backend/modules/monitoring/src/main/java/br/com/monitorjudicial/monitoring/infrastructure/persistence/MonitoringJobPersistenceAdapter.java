package br.com.monitorjudicial.monitoring.infrastructure.persistence;

import br.com.monitorjudicial.monitoring.domain.MonitoringJob;
import br.com.monitorjudicial.monitoring.domain.port.MonitoringJobRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class MonitoringJobPersistenceAdapter implements MonitoringJobRepository {

    private final MonitoringJobJpaRepository jpa;

    @Override
    public MonitoringJob save(MonitoringJob j) {
        MonitoringJobEntity e = new MonitoringJobEntity();
        e.setId(j.id());
        e.setExecutionId(j.executionId());
        e.setProcessId(j.processId());
        e.setExecutionDate(j.executionDate());
        e.setStatus(j.status());
        e.setExecutionTimeMs(j.executionTimeMs());
        e.setNewPublications(j.newPublications());
        e.setErrorMessage(j.errorMessage());
        e.setCreatedAt(j.createdAt());
        jpa.save(e);
        return j;
    }

    @Override
    public List<MonitoringJob> findByExecution(UUID executionId) {
        return jpa.findByExecutionIdOrderByCreatedAt(executionId).stream()
                .map(MonitoringJobPersistenceAdapter::toDomain).toList();
    }

    @Override
    public Map<UUID, MonitoringJob> lastJobByProcess(Collection<UUID> processIds) {
        if (processIds.isEmpty()) {
            return Map.of();
        }
        return jpa.findLastByProcess(processIds).stream().map(MonitoringJobPersistenceAdapter::toDomain)
                .collect(Collectors.toMap(MonitoringJob::processId, Function.identity()));
    }

    @Override
    public Set<UUID> processIdsWithFailedLastJob() {
        return Set.copyOf(jpa.processIdsWithFailedLastJob());
    }

    private static MonitoringJob toDomain(MonitoringJobEntity e) {
        return new MonitoringJob(e.getId(), e.getExecutionId(), e.getProcessId(), e.getExecutionDate(), e.getStatus(),
                e.getExecutionTimeMs(), e.getNewPublications(), e.getErrorMessage(), e.getCreatedAt());
    }
}
