package br.com.monitorjudicial.audit.application;

import br.com.monitorjudicial.audit.domain.AuditLog;
import br.com.monitorjudicial.audit.domain.port.AuditLogRepository;
import br.com.monitorjudicial.audit.domain.port.AuditLogRepository.AuditFilter;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import br.com.monitorjudicial.shared.audit.AuditableActionPerformed;
import br.com.monitorjudicial.shared.domain.Ids;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persiste ações auditáveis. O listener é síncrono: roda na transação de quem publicou, então a auditoria é gravada
 * (ou desfeita) junto com a escrita de negócio.
 */
@Service
public class AuditService {

    private final AuditLogRepository logs;

    public AuditService(AuditLogRepository logs) {
        this.logs = logs;
    }

    @EventListener
    public void on(AuditableActionPerformed event) {
        logs.save(new AuditLog(Ids.newId(), event.actorId(), event.actorEmail(), event.action(), event.resourceType(),
                event.resourceId(), event.details(), event.correlationId(), event.occurredAt()));
    }

    @Transactional(readOnly = true)
    public PageResult<AuditLog> search(AuditFilter filter, PageQuery page) {
        return logs.search(filter, page);
    }
}
