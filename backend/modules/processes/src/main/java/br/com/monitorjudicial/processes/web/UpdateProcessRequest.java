package br.com.monitorjudicial.processes.web;

import br.com.monitorjudicial.processes.domain.ProcessStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateProcessRequest(@Size(max = 300) String subject, @NotNull ProcessStatus status) {
}
