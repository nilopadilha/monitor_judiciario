package br.com.monitorjudicial.monitoring.web;

import br.com.monitorjudicial.monitoring.application.MonitoringExecutionView;
import br.com.monitorjudicial.monitoring.application.MonitoringQueryService;
import br.com.monitorjudicial.monitoring.application.RunMonitoringService;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Expõe {@code /api/v1/monitoring-executions} (padrão PJe, ADR-0003) e mantém aliases {@code /api/v1/monitoring} do PDF. */
@Tag(name = "Monitoramento")
@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@RequiredArgsConstructor
public class MonitoringExecutionController {

    private final RunMonitoringService runMonitoring;
    private final MonitoringQueryService queries;

    @Operation(summary = "Lista execuções de monitoramento, mais recentes primeiro")
    @GetMapping("/monitoring-executions")
    public PageResult<MonitoringExecutionView> list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return queries.list(PageQuery.of(page, size));
    }

    @Hidden
    @GetMapping("/monitoring")
    public PageResult<MonitoringExecutionView> listAlias(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return list(page, size);
    }

    @Operation(summary = "Detalha execução com o resultado por processo")
    @GetMapping("/monitoring-executions/{id}")
    public MonitoringExecutionView get(@PathVariable UUID id) {
        return queries.get(id);
    }

    @Operation(summary = "Inicia uma execução manual (assíncrona). 409 se já houver uma em andamento")
    @ApiResponse(responseCode = "202", description = "Accepted")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/monitoring-executions")
    public ResponseEntity<MonitoringExecutionView> start() {
        MonitoringExecutionView execution = runMonitoring.startManual();
        return ResponseEntity.accepted()
                .location(URI.create("/api/v1/monitoring-executions/" + execution.id()))
                .body(execution);
    }

    @Hidden
    @PostMapping("/monitoring/run")
    public ResponseEntity<MonitoringExecutionView> startAlias() {
        return start();
    }
}
