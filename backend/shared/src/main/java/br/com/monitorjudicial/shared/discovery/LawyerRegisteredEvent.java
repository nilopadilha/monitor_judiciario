package br.com.monitorjudicial.shared.discovery;

import java.time.Instant;
import java.util.UUID;

/** Evento emitido ao cadastrar advogado para disparar descoberta automática por OAB/UF no DJEN. */
public record LawyerRegisteredEvent(UUID lawyerId, String lawyerName, String oabNumber, String oabUf,
        String practiceArea, Instant registeredAt) {
}
