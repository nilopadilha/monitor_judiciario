package br.com.monitorjudicial.authentication.web;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(@NotNull Boolean active) {
}
