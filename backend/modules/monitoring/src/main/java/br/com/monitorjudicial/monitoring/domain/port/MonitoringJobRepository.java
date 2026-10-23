package br.com.monitorjudicial.monitoring.domain.port;

import br.com.monitorjudicial.monitoring.domain.MonitoringJob;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface MonitoringJobRepository {

    MonitoringJob save(MonitoringJob job);

    List<MonitoringJob> findByExecution(UUID executionId);

    /** Último job de cada processo informado. */
    Map<UUID, MonitoringJob> lastJobByProcess(Collection<UUID> processIds);

    Set<UUID> processIdsWithFailedLastJob();
}
