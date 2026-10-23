package br.com.monitorjudicial.authentication.web;

import jakarta.validation.constraints.NotNull;

/** {@code PASSWORD} exige email e password; {@code REFRESH_TOKEN} exige refreshToken. */
public record TokenRequest(@NotNull GrantType grantType, String email, String password, String refreshToken) {

    public enum GrantType {
        PASSWORD,
        REFRESH_TOKEN
    }
}
