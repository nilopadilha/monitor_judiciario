package br.com.monitorjudicial.lawyers.application;

import br.com.monitorjudicial.lawyers.domain.PracticeArea;
import java.util.UUID;

public record LawyerCommand(
        String name,
        String oabNumber,
        String oabUf,
        PracticeArea practiceArea,
        String email,
        String phone,
        UUID userId,
        boolean notifyEmail,
        boolean notifyWhatsapp) {
}
