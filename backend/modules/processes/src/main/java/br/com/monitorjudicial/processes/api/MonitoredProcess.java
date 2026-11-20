package br.com.monitorjudicial.processes.api;

import java.time.Instant;
import java.util.UUID;

/** Processo elegível para monitoramento. {@code number} contém somente dígitos. */
public record MonitoredProcess(UUID id, UUID clientId, UUID lawyerId, String number, String formattedNumber,
        String courtCode, boolean confidential, Instant lastCheckedAt, Instant createdAt) {

    public MonitoredProcess(UUID id, UUID clientId, String number, String formattedNumber, String courtCode,
            Instant lastCheckedAt, Instant createdAt) {
        this(id, clientId, null, number, formattedNumber, courtCode, false, lastCheckedAt, createdAt);
    }

    public MonitoredProcess(UUID id, UUID clientId, String number, String formattedNumber, String courtCode,
            boolean confidential, Instant lastCheckedAt, Instant createdAt) {
        this(id, clientId, null, number, formattedNumber, courtCode, confidential, lastCheckedAt, createdAt);
    }
}
