package br.com.monitorjudicial.authentication.web;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/** Emite e limpa o cookie {@code HttpOnly} do refresh token sem expor o segredo ao JavaScript. */
@Component
class RefreshCookieSupport {

    static final String COOKIE_NAME = "mj_refresh";

    private final boolean secure;
    private final String sameSite;
    private final Duration refreshValidity;

    RefreshCookieSupport(
            @Value("${security.cookie.secure:false}") boolean secure,
            @Value("${security.cookie.same-site:Strict}") String sameSite,
            @Value("${security.jwt.refresh-token-validity:P7D}") Duration refreshValidity) {
        this.secure = secure;
        this.sameSite = sameSite;
        this.refreshValidity = refreshValidity;
    }

    ResponseCookie issue(String refreshToken) {
        return ResponseCookie.from(COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/api/v1")
                .maxAge(refreshValidity)
                .build();
    }

    ResponseCookie clear() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/api/v1")
                .maxAge(0)
                .build();
    }
}
