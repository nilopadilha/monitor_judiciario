package br.com.monitorjudicial.lawyers.api;

import java.util.UUID;

public record LawyerSummary(
        UUID id,
        String name,
        String oabNumber,
        String oabUf,
        String practiceArea,
        String email,
        String phone,
        UUID userId,
        boolean notifyEmail,
        boolean notifyWhatsapp,
        boolean active) {
}
