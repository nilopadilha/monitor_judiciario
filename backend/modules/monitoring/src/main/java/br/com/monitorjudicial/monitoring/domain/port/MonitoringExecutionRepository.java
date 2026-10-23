package br.com.monitorjudicial.monitoring.domain.port;

import br.com.monitorjudicial.monitoring.domain.MonitoringExecution;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface MonitoringExecutionRepository {

    MonitoringExecution save(MonitoringExecution execution);

    Optional<MonitoringExecution> findById(UUID id);

    PageResult<MonitoringExecution> findRecent(PageQuery page);

    /** Marca como ABORTED execuções RUNNING órfãs (queda da aplicação durante a execução). */
    int abortRunning(Instant now);
}
