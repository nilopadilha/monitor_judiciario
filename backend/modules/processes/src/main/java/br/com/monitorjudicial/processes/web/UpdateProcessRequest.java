package br.com.monitorjudicial.processes.web;

import br.com.monitorjudicial.processes.domain.ProcessStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateProcessRequest(@Size(max = 300) String subject, @NotNull ProcessStatus status, Boolean confidential,
        UUID lawyerId, @Size(max = 32) String practiceArea) {
}
