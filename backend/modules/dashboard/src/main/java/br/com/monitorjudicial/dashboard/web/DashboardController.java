package br.com.monitorjudicial.dashboard.web;

import br.com.monitorjudicial.dashboard.application.DashboardService;
import br.com.monitorjudicial.dashboard.application.DashboardSummary;
import br.com.monitorjudicial.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Painel")
@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboard;

    @Operation(summary = "Indicadores do painel; CUSTOMER recebe apenas os dados do seu cliente")
    @GetMapping("/api/v1/dashboard-summary")
    public DashboardSummary summary(@AuthenticationPrincipal CurrentUser user) {
        return dashboard.summary(user);
    }
}
