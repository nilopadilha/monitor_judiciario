package br.com.monitorjudicial.authentication.web;

import br.com.monitorjudicial.authentication.application.AuthenticationService;
import br.com.monitorjudicial.authentication.application.TokenPair;
import br.com.monitorjudicial.shared.domain.BusinessRuleViolationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
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
    private final RefreshCookieSupport cookies;

    @Operation(summary = "Emite tokens por senha (login) ou por refresh token (renovação com rotação)")
    @PostMapping
    public ResponseEntity<TokenPair> create(@Valid @RequestBody TokenRequest request,
            @CookieValue(name = RefreshCookieSupport.COOKIE_NAME, required = false) String refreshCookie) {
        TokenPair pair = switch (request.grantType()) {
            case PASSWORD -> {
                if (request.email() == null || request.password() == null) {
                    throw new BusinessRuleViolationException("missing-credentials", "Informe email e password.");
                }
                yield authentication.authenticate(request.email(), request.password());
            }
            case REFRESH_TOKEN -> {
                String token = resolveRefreshToken(request.refreshToken(), refreshCookie);
                if (token == null) {
                    throw new BusinessRuleViolationException("missing-refresh-token", "Informe refreshToken.");
                }
                yield authentication.refresh(token);
            }
        };
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, cookies.issue(pair.refreshToken()).toString())
                .body(pair);
    }

    private static String resolveRefreshToken(String bodyToken, String cookieToken) {
        if (bodyToken != null && !bodyToken.isBlank()) {
            return bodyToken;
        }
        return cookieToken != null && !cookieToken.isBlank() ? cookieToken : null;
    }
}
