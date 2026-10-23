package br.com.monitorjudicial.authentication.web;

import br.com.monitorjudicial.authentication.application.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Autenticação")
@RestController
@RequestMapping("/api/v1/token-revocations")
@RequiredArgsConstructor
public class TokenRevocationController {

    private final AuthenticationService authentication;

    @Operation(summary = "Revoga o refresh token e toda a sua cadeia (logout), no modelo da RFC 7009")
    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@Valid @RequestBody TokenRevocationRequest request) {
        authentication.revoke(request.refreshToken());
    }
}
