package br.com.monitorjudicial.clients.web;

import br.com.monitorjudicial.clients.application.ClientCommand;
import br.com.monitorjudicial.clients.application.ClientService;
import br.com.monitorjudicial.clients.application.ClientView;
import br.com.monitorjudicial.clients.domain.ClientStatus;
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

@Tag(name = "Clientes")
@RestController
@RequestMapping("/api/v1/clients")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clients;

    @Operation(summary = "Lista clientes (busca por nome ou documento)")
    @GetMapping
    public PageResult<ClientView> list(@RequestParam(required = false) String q,
            @RequestParam(required = false) ClientStatus status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return clients.search(q, status, PageQuery.of(page, size));
    }

    @Operation(summary = "Detalha cliente")
    @GetMapping("/{id}")
    public ClientView get(@PathVariable UUID id) {
        return clients.get(id);
    }

    @Operation(summary = "Cadastra cliente")
    @PostMapping
    public ResponseEntity<ClientView> create(@Valid @RequestBody ClientRequest request) {
        ClientView created = clients.create(toCommand(request));
        return ResponseEntity.created(URI.create("/api/v1/clients/" + created.id())).body(created);
    }

    @Operation(summary = "Altera cliente (o documento não muda)")
    @PutMapping("/{id}")
    public ClientView update(@PathVariable UUID id, @Valid @RequestBody ClientRequest request) {
        return clients.update(id, toCommand(request));
    }

    @Operation(summary = "Inativa cliente (exclusão lógica, preserva histórico)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        clients.deactivate(id);
    }

    private static ClientCommand toCommand(ClientRequest r) {
        return new ClientCommand(r.name(), r.document(), r.email(), r.phone(), r.notifyEmail(), r.notifyWhatsapp());
    }
}
