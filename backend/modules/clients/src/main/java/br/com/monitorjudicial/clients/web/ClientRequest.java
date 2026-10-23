package br.com.monitorjudicial.clients.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClientRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank String document,
        @NotBlank @Email String email,
        @Size(max = 20) String phone,
        boolean notifyEmail,
        boolean notifyWhatsapp) {
}
