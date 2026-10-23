package br.com.monitorjudicial.tribunals.web;

import jakarta.validation.constraints.NotNull;

public record TribunalPatchRequest(@NotNull Boolean active) {
}
