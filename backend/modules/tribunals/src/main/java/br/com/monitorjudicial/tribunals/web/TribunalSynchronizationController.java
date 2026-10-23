package br.com.monitorjudicial.tribunals.web;

import br.com.monitorjudicial.tribunals.application.SynchronizationResult;
import br.com.monitorjudicial.tribunals.application.TribunalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Tribunais")
@RestController
@RequestMapping("/api/v1/tribunal-synchronizations")
@RequiredArgsConstructor
public class TribunalSynchronizationController {

    private final TribunalService tribunals;

    @Operation(summary = "Sincroniza a lista de tribunais estaduais com o DJEN")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public SynchronizationResult synchronize() {
        return tribunals.synchronize();
    }
}
