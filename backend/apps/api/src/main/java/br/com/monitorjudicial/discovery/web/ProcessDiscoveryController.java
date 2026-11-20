package br.com.monitorjudicial.discovery.web;

import br.com.monitorjudicial.discovery.application.DiscoveryResult;
import br.com.monitorjudicial.discovery.application.ProcessDiscoveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Descoberta Automática")
@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@RequiredArgsConstructor
public class ProcessDiscoveryController {

    private final ProcessDiscoveryService discovery;

    @Operation(summary = "Busca processos no DJEN pelo nome do cliente e auto-vincula imediatamente")
    @PostMapping("/clients/{id}/process-discoveries")
    public DiscoveryResult discoverForClient(@PathVariable UUID id) {
        return discovery.discoverForClient(id);
    }

    @Operation(summary = "Busca processos no DJEN pela OAB/UF do advogado e auto-vincula")
    @PostMapping("/lawyers/{id}/process-discoveries")
    public DiscoveryResult discoverForLawyer(@PathVariable UUID id) {
        return discovery.discoverForLawyer(id);
    }
}
