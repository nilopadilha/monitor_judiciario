package br.com.monitorjudicial.tribunals.web;

import br.com.monitorjudicial.tribunals.application.TribunalService;
import br.com.monitorjudicial.tribunals.application.TribunalView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Tribunais")
@RestController
@RequestMapping("/api/v1/tribunals")
@RequiredArgsConstructor
public class TribunalController {

    private final TribunalService tribunals;

    @Operation(summary = "Lista tribunais")
    @GetMapping
    public List<TribunalView> list(@RequestParam(required = false) Boolean active) {
        return tribunals.list(active);
    }

    @Operation(summary = "Ativa ou desativa o monitoramento de um tribunal")
    @PatchMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public TribunalView patch(@PathVariable String code, @Valid @RequestBody TribunalPatchRequest request) {
        return tribunals.setActive(code, request.active());
    }
}
