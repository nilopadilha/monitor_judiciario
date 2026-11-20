package br.com.monitorjudicial.lgpd.web;

import br.com.monitorjudicial.lgpd.application.ClientDataExport;
import br.com.monitorjudicial.lgpd.application.LgpdDataExportService;
import br.com.monitorjudicial.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "LGPD")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class LgpdController {

    private final LgpdDataExportService exportService;

    @Operation(summary = "Exporta dados pessoais do cliente em JSON (Art. 18, V LGPD)")
    @GetMapping("/clients/{id}/data-export")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','CUSTOMER')")
    public ClientDataExport exportClient(@PathVariable UUID id, @AuthenticationPrincipal CurrentUser user) {
        return exportService.exportForClient(id, user);
    }

    @Operation(summary = "Exporta os próprios dados pessoais do cliente autenticado (CUSTOMER)")
    @GetMapping("/users/me/data-export")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ClientDataExport exportMine(@AuthenticationPrincipal CurrentUser user) {
        return exportService.exportForCurrentCustomer(user);
    }
}
