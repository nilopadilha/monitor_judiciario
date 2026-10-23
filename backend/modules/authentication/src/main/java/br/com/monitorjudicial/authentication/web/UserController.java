package br.com.monitorjudicial.authentication.web;

import br.com.monitorjudicial.authentication.application.CreateUserCommand;
import br.com.monitorjudicial.authentication.application.UserService;
import br.com.monitorjudicial.authentication.application.UserView;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import br.com.monitorjudicial.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Usuários")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService users;

    @Operation(summary = "Lista usuários")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageResult<UserView> list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return users.list(PageQuery.of(page, size));
    }

    @Operation(summary = "Cria usuário (CUSTOMER exige clientId)")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserView> create(@Valid @RequestBody CreateUserRequest request) {
        UserView created = users.create(new CreateUserCommand(request.name(), request.email(), request.password(),
                request.role(), request.clientId()));
        return ResponseEntity.created(URI.create("/api/v1/users/" + created.id())).body(created);
    }

    @Operation(summary = "Usuário autenticado")
    @GetMapping("/me")
    public UserView me(@AuthenticationPrincipal CurrentUser user) {
        return users.get(user.userId());
    }

    @Operation(summary = "Detalha usuário")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserView get(@PathVariable UUID id) {
        return users.get(id);
    }
}
