package br.com.monitorjudicial.deadlines.application;

import br.com.monitorjudicial.deadlines.domain.DeadlineStatus;
import br.com.monitorjudicial.deadlines.domain.DeadlineUrgency;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DeadlineView(
        UUID id,
        UUID processId,
        String processNumber,
        String courtCode,
        String clientName,
        UUID publicationId,
        UUID lawyerId,
        String lawyerName,
        String title,
        String description,
        LocalDate startDate,
        LocalDate dueDate,
        Integer daysCount,
        boolean businessDays,
        DeadlineStatus status,
        DeadlineUrgency urgency,
        long daysRemaining,
        boolean autoDetected,
        Instant completedAt,
        Instant createdAt) {
}
