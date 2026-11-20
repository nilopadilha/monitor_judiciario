package br.com.monitorjudicial.shared.discovery;

import java.time.Instant;
import java.util.UUID;

/** Evento emitido ao cadastrar cliente para disparar descoberta automática de processos no DJEN. */
public record ClientRegisteredEvent(UUID clientId, String clientName, Instant registeredAt) {
}
