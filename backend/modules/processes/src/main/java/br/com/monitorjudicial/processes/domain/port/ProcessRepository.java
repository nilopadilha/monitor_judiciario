package br.com.monitorjudicial.processes.domain.port;

import br.com.monitorjudicial.processes.domain.Process;
import br.com.monitorjudicial.processes.domain.ProcessNumber;
import br.com.monitorjudicial.processes.domain.ProcessStatus;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProcessRepository {

    Process save(Process process);

    Optional<Process> findById(UUID id);

    List<Process> findAllById(Collection<UUID> ids);

    boolean existsByNumber(ProcessNumber number);

    List<Process> findByStatusOrderByLastCheckedAt(ProcessStatus status);

    void updateLastCheckedAt(UUID id, Instant checkedAt);

    PageResult<Process> search(ProcessFilter filter, PageQuery page);

    /** {@code restrictTo} nulo = sem restrição; vazio = nenhum resultado. */
    record ProcessFilter(UUID clientId, ProcessStatus status, String courtCode, String number,
            Collection<UUID> restrictTo) {
    }
}
