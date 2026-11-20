package br.com.monitorjudicial.lawyers.web;

import br.com.monitorjudicial.lawyers.application.LawyerCommand;
import br.com.monitorjudicial.lawyers.application.LawyerService;
import br.com.monitorjudicial.lawyers.application.LawyerView;
import br.com.monitorjudicial.lawyers.domain.PracticeArea;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

@Tag(name = "Advogados")
@RestController
@RequestMapping("/api/v1/lawyers")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@RequiredArgsConstructor
public class LawyerController {

    private final LawyerService lawyers;

    @Operation(summary = "Lista advogados do escritório (busca por nome, OAB ou área)")
    @GetMapping
    public PageResult<LawyerView> list(@RequestParam(required = false) String q,
            @RequestParam(required = false) PracticeArea practiceArea,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return lawyers.search(q, practiceArea, active, PageQuery.of(page, size));
    }

    @Operation(summary = "Detalha advogado")
    @GetMapping("/{id}")
    public LawyerView get(@PathVariable UUID id) {
        return lawyers.get(id);
    }

    @Operation(summary = "Cadastra advogado (OAB + UF + área do direito)")
    @PostMapping
    public ResponseEntity<LawyerView> create(@Valid @RequestBody LawyerRequest request) {
        LawyerView created = lawyers.create(toCommand(request));
        return ResponseEntity.created(URI.create("/api/v1/lawyers/" + created.id())).body(created);
    }

    @Operation(summary = "Altera dados do advogado (a OAB é imutável)")
    @PutMapping("/{id}")
    public LawyerView update(@PathVariable UUID id, @Valid @RequestBody LawyerRequest request) {
        return lawyers.update(id, toCommand(request));
    }

    @Operation(summary = "Inativa advogado")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable UUID id) {
        lawyers.deactivate(id);
    }

    private static LawyerCommand toCommand(LawyerRequest r) {
        return new LawyerCommand(r.name(), r.oabNumber(), r.oabUf(), r.practiceArea(), r.email(), r.phone(),
                r.userId(), r.notifyEmail(), r.notifyWhatsapp());
    }
}
