package br.com.monitorjudicial.authentication.web;

import br.com.monitorjudicial.authentication.application.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Autenticação")
@RestController
@RequestMapping("/api/v1/token-revocations")
@RequiredArgsConstructor
public class TokenRevocationController {

    private final AuthenticationService authentication;
    private final RefreshCookieSupport cookies;

    @Operation(summary = "Revoga o refresh token e toda a sua cadeia (logout), no modelo da RFC 7009")
    @PostMapping
    public ResponseEntity<Void> revoke(@RequestBody(required = false) TokenRevocationRequest request,
            @CookieValue(name = RefreshCookieSupport.COOKIE_NAME, required = false) String refreshCookie) {
        String bodyToken = request == null ? null : request.refreshToken();
        String token = bodyToken != null && !bodyToken.isBlank() ? bodyToken : refreshCookie;
        if (token != null && !token.isBlank()) {
            authentication.revoke(token);
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookies.clear().toString())
                .build();
    }
}
