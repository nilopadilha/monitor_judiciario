package br.com.monitorjudicial.monitoring.domain.port;

import br.com.monitorjudicial.monitoring.domain.Movement;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import java.util.UUID;

public interface MovementRepository {

    Movement save(Movement movement);

    PageResult<Movement> findByProcess(UUID processId, PageQuery page);
}
