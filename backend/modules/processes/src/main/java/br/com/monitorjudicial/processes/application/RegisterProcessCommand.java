package br.com.monitorjudicial.processes.application;

import java.util.UUID;

public record RegisterProcessCommand(UUID clientId, UUID lawyerId, String practiceArea, String number, String subject,
        boolean confidential) {

    public RegisterProcessCommand(UUID clientId, String number, String subject) {
        this(clientId, null, null, number, subject, false);
    }

    public RegisterProcessCommand(UUID clientId, String number, String subject, boolean confidential) {
        this(clientId, null, null, number, subject, confidential);
    }
}
