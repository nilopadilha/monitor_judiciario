package br.com.monitorjudicial.deadlines.web;

import br.com.monitorjudicial.deadlines.application.CreateDeadlineCommand;
import br.com.monitorjudicial.deadlines.application.DeadlineService;
import br.com.monitorjudicial.deadlines.application.DeadlineSummary;
import br.com.monitorjudicial.deadlines.application.DeadlineView;
import br.com.monitorjudicial.deadlines.domain.DeadlineStatus;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Prazos Processuais")
@RestController
@RequestMapping("/api/v1/deadlines")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@RequiredArgsConstructor
public class DeadlineController {

    private final DeadlineService deadlines;

    @Operation(summary = "Lista prazos processuais ordenados pela data fatal")
    @GetMapping
    public PageResult<DeadlineView> list(@RequestParam(required = false) UUID lawyerId,
            @RequestParam(required = false) UUID processId,
            @RequestParam(required = false) DeadlineStatus status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return deadlines.search(lawyerId, processId, status, PageQuery.of(page, size));
    }

    @Operation(summary = "Resumo de prazos críticos (vencidos, hoje, 3 dias, 7 dias)")
    @GetMapping("/summary")
    public DeadlineSummary summary() {
        return deadlines.summary();
    }

    @Operation(summary = "Cadastra prazo processual (por data fatal ou contagem de dias úteis/corridos)")
    @PostMapping
    public ResponseEntity<DeadlineView> create(@Valid @RequestBody CreateDeadlineRequest request) {
        DeadlineView created = deadlines.create(new CreateDeadlineCommand(
                request.processId(), request.lawyerId(), request.title(), request.description(),
                request.startDate(), request.dueDate(), request.daysCount(),
                request.businessDays() == null || request.businessDays()));
        return ResponseEntity.created(URI.create("/api/v1/deadlines/" + created.id())).body(created);
    }

    @Operation(summary = "Atualiza status do prazo (OPEN, COMPLETED, CANCELED)")
    @PatchMapping("/{id}")
    public DeadlineView updateStatus(@PathVariable UUID id, @Valid @RequestBody UpdateDeadlineStatusRequest request) {
        return deadlines.updateStatus(id, request.status());
    }

    @Operation(summary = "Dispara alertas de prazos vencidos/próximos para os advogados responsáveis")
    @PostMapping("/alerts")
    public Map<String, Integer> triggerAlerts() {
        return Map.of("alertsSent", deadlines.sendDueAlerts());
    }
}
