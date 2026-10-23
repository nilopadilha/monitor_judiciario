package br.com.monitorjudicial.processes.web;

import br.com.monitorjudicial.processes.application.MonitoringSituation;
import br.com.monitorjudicial.processes.application.ProcessSearch;
import br.com.monitorjudicial.processes.application.ProcessService;
import br.com.monitorjudicial.processes.application.ProcessView;
import br.com.monitorjudicial.processes.application.RegisterProcessCommand;
import br.com.monitorjudicial.processes.domain.ProcessStatus;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import br.com.monitorjudicial.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Processos")
@RestController
@RequestMapping("/api/v1/processes")
@RequiredArgsConstructor
public class ProcessController {

    private final ProcessService processes;

    @Operation(summary = "Lista processos (CUSTOMER vê apenas os do seu cliente)")
    @GetMapping
    public PageResult<ProcessView> list(@RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) ProcessStatus status,
            @RequestParam(required = false) String courtCode,
            @RequestParam(required = false) String number,
            @RequestParam(required = false) MonitoringSituation situation,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CurrentUser user) {
        return processes.search(new ProcessSearch(clientId, status, courtCode, number, situation),
                PageQuery.of(page, size), user);
    }

    @Operation(summary = "Detalha processo")
    @GetMapping("/{id}")
    public ProcessView get(@PathVariable UUID id, @AuthenticationPrincipal CurrentUser user) {
        return processes.get(id, user);
    }

    @Operation(summary = "Cadastra processo pelo número CNJ; o tribunal é derivado do número")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ProcessView> create(@Valid @RequestBody RegisterProcessRequest request) {
        ProcessView created = processes.register(
                new RegisterProcessCommand(request.clientId(), request.number(), request.subject()));
        return ResponseEntity.created(URI.create("/api/v1/processes/" + created.id())).body(created);
    }

    @Operation(summary = "Altera assunto e status (ACTIVE/PAUSED)")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ProcessView update(@PathVariable UUID id, @Valid @RequestBody UpdateProcessRequest request) {
        return processes.update(id, request.subject(), request.status());
    }

    @Operation(summary = "Arquiva processo (encerra o monitoramento, preserva histórico)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public void archive(@PathVariable UUID id) {
        processes.archive(id);
    }
}
