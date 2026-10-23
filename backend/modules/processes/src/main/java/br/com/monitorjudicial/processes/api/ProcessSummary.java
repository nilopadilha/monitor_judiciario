package br.com.monitorjudicial.processes.api;

import java.util.UUID;

public record ProcessSummary(UUID id, UUID clientId, String formattedNumber, String courtCode, String subject) {
}
