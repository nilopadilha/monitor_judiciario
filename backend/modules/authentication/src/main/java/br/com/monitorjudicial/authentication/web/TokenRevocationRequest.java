package br.com.monitorjudicial.authentication.web;

import jakarta.validation.constraints.NotBlank;

public record TokenRevocationRequest(@NotBlank String refreshToken) {
}
