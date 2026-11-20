package br.com.monitorjudicial.deadlines.application;

import java.time.LocalDate;
import java.util.UUID;

public record CreateDeadlineCommand(
        UUID processId,
        UUID lawyerId,
        String title,
        String description,
        LocalDate startDate,
        LocalDate dueDate,
        Integer daysCount,
        boolean businessDays) {
}
