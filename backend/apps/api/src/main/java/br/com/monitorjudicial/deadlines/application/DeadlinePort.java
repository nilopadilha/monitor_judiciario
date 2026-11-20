package br.com.monitorjudicial.deadlines.application;

import br.com.monitorjudicial.deadlines.domain.DeadlineStatus;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeadlinePort {

    boolean existsForPublication(UUID publicationId);

    void insert(UUID id, UUID processId, UUID publicationId, UUID lawyerId, String title, String description,
            LocalDate startDate, LocalDate dueDate, Integer daysCount, boolean businessDays, boolean autoDetected,
            Instant now);

    Optional<DeadlineView> findById(UUID id, LocalDate today);

    PageResult<DeadlineView> search(UUID lawyerId, UUID processId, DeadlineStatus status, PageQuery page,
            LocalDate today);

    DeadlineSummary summary(LocalDate today);

    boolean updateStatus(UUID id, DeadlineStatus status, Instant completedAt, Instant now);

    boolean tryAcquireAlertLock();

    void releaseAlertLock();

    List<DueDeadlineAlert> findDueForAlert(LocalDate maxDueDate, LocalDate today);

    void markAlerted(UUID deadlineId, LocalDate today, Instant now);

    record DueDeadlineAlert(UUID deadlineId, String processNumber, String clientName, String title, LocalDate dueDate,
            String lawyerName, String lawyerEmail, boolean notifyEmail) {
    }
}
