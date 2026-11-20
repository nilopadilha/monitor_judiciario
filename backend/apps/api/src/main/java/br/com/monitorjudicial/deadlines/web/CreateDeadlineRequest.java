package br.com.monitorjudicial.deadlines.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record CreateDeadlineRequest(
        @NotNull UUID processId,
        UUID lawyerId,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description,
        LocalDate startDate,
        LocalDate dueDate,
        Integer daysCount,
        Boolean businessDays) {
}
