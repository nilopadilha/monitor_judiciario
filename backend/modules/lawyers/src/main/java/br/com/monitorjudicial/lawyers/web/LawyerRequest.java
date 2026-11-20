package br.com.monitorjudicial.lawyers.web;

import br.com.monitorjudicial.lawyers.domain.PracticeArea;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record LawyerRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 20) String oabNumber,
        @NotBlank @Size(min = 2, max = 2) String oabUf,
        PracticeArea practiceArea,
        @NotBlank @Email String email,
        @Size(max = 20) String phone,
        UUID userId,
        boolean notifyEmail,
        boolean notifyWhatsapp) {
}
