package br.com.monitorjudicial.audit.domain.port;

import br.com.monitorjudicial.audit.domain.AuditLog;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import java.time.Instant;
import java.util.UUID;

public interface AuditLogRepository {

    void save(AuditLog log);

    PageResult<AuditLog> search(AuditFilter filter, PageQuery page);

    record AuditFilter(String resourceType, String resourceId, UUID actorId, Instant from, Instant to) {
    }
}
