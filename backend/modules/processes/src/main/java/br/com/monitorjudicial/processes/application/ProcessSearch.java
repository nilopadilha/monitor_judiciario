package br.com.monitorjudicial.processes.application;

import br.com.monitorjudicial.processes.domain.ProcessStatus;
import java.util.UUID;

public record ProcessSearch(UUID clientId, ProcessStatus status, String courtCode, String number,
        MonitoringSituation situation) {
}
