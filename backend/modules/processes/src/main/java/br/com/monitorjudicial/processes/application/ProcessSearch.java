package br.com.monitorjudicial.processes.application;

import br.com.monitorjudicial.processes.domain.ProcessStatus;
import java.util.UUID;

public record ProcessSearch(UUID clientId, UUID lawyerId, ProcessStatus status, String courtCode, String number,
        MonitoringSituation situation) {

    public ProcessSearch(UUID clientId, ProcessStatus status, String courtCode, String number,
            MonitoringSituation situation) {
        this(clientId, null, status, courtCode, number, situation);
    }
}
