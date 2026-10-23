package br.com.monitorjudicial.processes.api;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Contrato público do módulo de processos. */
public interface ProcessDirectory {

    /** Processos ACTIVE, em ordem de verificação mais antiga primeiro. */
    List<MonitoredProcess> findMonitorable();

    Optional<ProcessSummary> findById(UUID id);

    Map<UUID, ProcessSummary> findByIds(Collection<UUID> ids);

    void markChecked(UUID processId, Instant checkedAt);
}
