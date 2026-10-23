package br.com.monitorjudicial.audit.web;

import br.com.monitorjudicial.audit.application.AuditService;
import br.com.monitorjudicial.audit.domain.AuditLog;
import br.com.monitorjudicial.audit.domain.port.AuditLogRepository.AuditFilter;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auditoria")
@RestController
@RequestMapping("/api/v1/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditService audit;

    @Operation(summary = "Consulta a trilha de auditoria")
    @GetMapping
    public PageResult<AuditLog> list(@RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceId, @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        return audit.search(new AuditFilter(resourceType, resourceId, actorId, from, to), PageQuery.of(page, size));
    }
}
