package br.com.monitorjudicial.monitoring.application;

import br.com.monitorjudicial.monitoring.domain.port.MovementRepository;
import br.com.monitorjudicial.processes.api.ProcessDirectory;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import br.com.monitorjudicial.shared.domain.ResourceNotFoundException;
import br.com.monitorjudicial.shared.security.CurrentUser;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MovementQueryService {

    private final MovementRepository movements;
    private final ProcessDirectory processes;

    public MovementQueryService(MovementRepository movements, ProcessDirectory processes) {
        this.movements = movements;
        this.processes = processes;
    }

    @Transactional(readOnly = true)
    public PageResult<MovementView> list(UUID processId, PageQuery page, CurrentUser user) {
        processes.findById(processId)
                .filter(p -> user.dataScope().map(scope -> scope.equals(p.clientId())).orElse(true))
                .orElseThrow(() -> new ResourceNotFoundException("processo", processId));
        return movements.findByProcess(processId, page).map(MovementView::of);
    }
}
