package br.com.monitorjudicial.monitoring.web;

import br.com.monitorjudicial.monitoring.application.PublicationService;
import br.com.monitorjudicial.monitoring.application.PublicationView;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import br.com.monitorjudicial.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Publicações")
@RestController
@RequestMapping("/api/v1/publications")
@RequiredArgsConstructor
public class PublicationController {

    private final PublicationService publications;

    @Operation(summary = "Lista publicações (CUSTOMER vê apenas as do seu cliente)")
    @GetMapping
    public PageResult<PublicationView> list(@RequestParam(required = false) UUID processId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Boolean unread,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CurrentUser user) {
        return publications.search(processId, from, to, unread, PageQuery.of(page, size), user);
    }

    @Operation(summary = "Detalha publicação")
    @GetMapping("/{id}")
    public PublicationView get(@PathVariable UUID id, @AuthenticationPrincipal CurrentUser user) {
        return publications.get(id, user);
    }

    @Operation(summary = "Marca publicação como lida")
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public PublicationView patch(@PathVariable UUID id, @Valid @RequestBody PublicationPatchRequest request,
            @AuthenticationPrincipal CurrentUser user) {
        return publications.markRead(id, user);
    }
}
