package br.com.monitorjudicial.monitoring.web;

import br.com.monitorjudicial.monitoring.application.MovementQueryService;
import br.com.monitorjudicial.monitoring.application.MovementView;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import br.com.monitorjudicial.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Processos")
@RestController
@RequiredArgsConstructor
public class MovementController {

    private final MovementQueryService movements;

    @Operation(summary = "Linha do tempo de movimentações do processo")
    @GetMapping("/api/v1/processes/{processId}/movements")
    public PageResult<MovementView> list(@PathVariable UUID processId, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size, @AuthenticationPrincipal CurrentUser user) {
        return movements.list(processId, PageQuery.of(page, size), user);
    }
}
