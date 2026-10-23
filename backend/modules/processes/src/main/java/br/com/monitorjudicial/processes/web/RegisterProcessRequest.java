package br.com.monitorjudicial.processes.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record RegisterProcessRequest(@NotNull UUID clientId, @NotBlank String number, @Size(max = 300) String subject) {
}
