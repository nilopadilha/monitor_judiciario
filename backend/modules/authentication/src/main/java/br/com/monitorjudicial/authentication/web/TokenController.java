package br.com.monitorjudicial.authentication.web;

import br.com.monitorjudicial.authentication.application.AuthenticationService;
import br.com.monitorjudicial.authentication.application.TokenPair;
import br.com.monitorjudicial.shared.domain.BusinessRuleViolationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Autenticação")
@RestController
@RequestMapping("/api/v1/tokens")
@RequiredArgsConstructor
public class TokenController {

    private final AuthenticationService authentication;

    @Operation(summary = "Emite tokens por senha (login) ou por refresh token (renovação com rotação)")
    @PostMapping
    public ResponseEntity<TokenPair> create(@Valid @RequestBody TokenRequest request) {
        TokenPair pair = switch (request.grantType()) {
            case PASSWORD -> {
                if (request.email() == null || request.password() == null) {
                    throw new BusinessRuleViolationException("missing-credentials", "Informe email e password.");
                }
                yield authentication.authenticate(request.email(), request.password());
            }
            case REFRESH_TOKEN -> {
                if (request.refreshToken() == null || request.refreshToken().isBlank()) {
                    throw new BusinessRuleViolationException("missing-refresh-token", "Informe refreshToken.");
                }
                yield authentication.refresh(request.refreshToken());
            }
        };
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(pair);
    }
}
