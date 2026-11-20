package br.com.monitorjudicial.deadlines.web;

import br.com.monitorjudicial.deadlines.domain.DeadlineStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateDeadlineStatusRequest(@NotNull DeadlineStatus status) {
}
