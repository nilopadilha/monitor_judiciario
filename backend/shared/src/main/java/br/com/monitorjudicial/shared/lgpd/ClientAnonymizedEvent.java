package br.com.monitorjudicial.shared.lgpd;

import java.time.Instant;
import java.util.UUID;

/** Evento síncrono disparado na mesma transação quando um titular é anonimizado (Art. 18, LGPD). */
public record ClientAnonymizedEvent(UUID clientId, Instant anonymizedAt) {
}
