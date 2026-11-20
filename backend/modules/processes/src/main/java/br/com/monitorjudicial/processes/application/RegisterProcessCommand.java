package br.com.monitorjudicial.processes.application;

import java.util.UUID;

public record RegisterProcessCommand(UUID clientId, String number, String subject, boolean confidential) {

    public RegisterProcessCommand(UUID clientId, String number, String subject) {
        this(clientId, number, subject, false);
    }
}
