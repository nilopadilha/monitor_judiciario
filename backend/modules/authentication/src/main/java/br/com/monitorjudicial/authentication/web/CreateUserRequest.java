package br.com.monitorjudicial.authentication.web;

import br.com.monitorjudicial.shared.security.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateUserRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 12, max = 128) String password,
        @NotNull Role role,
        UUID clientId) {
}
