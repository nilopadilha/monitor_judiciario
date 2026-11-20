package br.com.monitorjudicial.deadlines.application;

import br.com.monitorjudicial.deadlines.domain.DeadlineCalculator;
import br.com.monitorjudicial.deadlines.domain.DeadlineStatus;
import br.com.monitorjudicial.monitoring.api.ProcessEvolutionDetected;
import br.com.monitorjudicial.processes.api.ProcessDirectory;
import br.com.monitorjudicial.processes.api.ProcessSummary;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import br.com.monitorjudicial.shared.audit.Auditor;
import br.com.monitorjudicial.shared.domain.BusinessRuleViolationException;
import br.com.monitorjudicial.shared.domain.Guard;
import br.com.monitorjudicial.shared.domain.Ids;
import br.com.monitorjudicial.shared.domain.ResourceNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Gestão, extração automática e alertas diários de prazos processuais para advogados. */
@Service
public class DeadlineService {

    private static final Logger log = LoggerFactory.getLogger(DeadlineService.class);
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");

    private final DeadlinePort port;
    private final ProcessDirectory processes;
    private final DeadlineAlertNotifier notifier;
    private final Auditor auditor;
    private final Clock clock;
    private final int alertAdvanceDays;

    public DeadlineService(DeadlinePort port, ProcessDirectory processes, DeadlineAlertNotifier notifier,
            Auditor auditor, Clock clock, @Value("${deadlines.alert-advance-days:5}") int alertAdvanceDays) {
        this.port = port;
        this.processes = processes;
        this.notifier = notifier;
        this.auditor = auditor;
        this.clock = clock;
        this.alertAdvanceDays = alertAdvanceDays <= 0 ? 5 : alertAdvanceDays;
    }

    @Transactional
    public DeadlineView create(CreateDeadlineCommand command) {
        ProcessSummary process = processes.findById(command.processId())
                .orElseThrow(() -> new ResourceNotFoundException("processo", command.processId()));
        String title = Guard.requireText(command.title(), "invalid-deadline-title", "Título do prazo é obrigatório.");
        LocalDate today = today();
        LocalDate start = command.startDate() == null ? today : command.startDate();
        LocalDate due;
        if (command.dueDate() != null) {
            due = command.dueDate();
        } else if (command.daysCount() != null && command.daysCount() > 0) {
            due = DeadlineCalculator.computeDueDate(start, command.daysCount(), command.businessDays());
        } else {
            throw new BusinessRuleViolationException("missing-due-date",
                    "Informe a data fatal (dueDate) ou a quantidade de dias (daysCount).");
        }
        UUID id = Ids.newId();
        UUID lawyerId = command.lawyerId() != null ? command.lawyerId() : process.lawyerId();
        Instant now = clock.instant();
        port.insert(id, process.id(), null, lawyerId, title, command.description(), start, due,
                command.daysCount(), command.businessDays(), false, now);
        auditor.record("CREATE", "deadline", id, Map.of("dueDate", due.toString()));
        return port.findById(id, today).orElseThrow();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registerFromEvolution(ProcessEvolutionDetected event) {
        if (event.publicationId() == null || port.existsForPublication(event.publicationId())) {
            return;
        }
        DeadlineCalculator.extractFromPublication(event.movementType(), event.movementLabel(), event.excerpt(),
                event.publicationDate()).ifPresent(detected -> {
            UUID id = Ids.newId();
            port.insert(id, event.processId(), event.publicationId(), event.lawyerId(), detected.title(),
                    event.confidential() ? "Processo sob segredo de justiça." : event.excerpt(),
                    detected.startDate(), detected.dueDate(), detected.daysCount(), detected.businessDays(),
                    true, clock.instant());
            auditor.record("AUTO_CREATE", "deadline", id,
                    Map.of("dueDate", detected.dueDate().toString(), "days", detected.daysCount()));
        });
    }

    @Transactional
    public DeadlineView updateStatus(UUID id, DeadlineStatus status) {
        LocalDate today = today();
        Instant now = clock.instant();
        Instant completedAt = status == DeadlineStatus.COMPLETED ? now : null;
        if (!port.updateStatus(id, status, completedAt, now)) {
            throw new ResourceNotFoundException("prazo", id);
        }
        auditor.record("UPDATE_STATUS", "deadline", id, Map.of("status", status.name()));
        return port.findById(id, today).orElseThrow();
    }

    @Transactional(readOnly = true)
    public PageResult<DeadlineView> search(UUID lawyerId, UUID processId, DeadlineStatus status, PageQuery page) {
        return port.search(lawyerId, processId, status, page, today());
    }

    @Transactional(readOnly = true)
    public DeadlineSummary summary() {
        return port.summary(today());
    }

    @Transactional
    public int sendDueAlerts() {
        if (!port.tryAcquireAlertLock()) {
            return 0;
        }
        try {
            LocalDate today = today();
            List<DeadlinePort.DueDeadlineAlert> dueList = port.findDueForAlert(today.plusDays(alertAdvanceDays), today);
            int sent = 0;
            for (var alert : dueList) {
                if (!alert.notifyEmail() || alert.lawyerEmail() == null || alert.lawyerEmail().isBlank()) {
                    continue;
                }
                long daysRemaining = ChronoUnit.DAYS.between(today, alert.dueDate());
                try {
                    notifier.notifyLawyer(alert, daysRemaining);
                    port.markAlerted(alert.deadlineId(), today, clock.instant());
                    sent++;
                } catch (RuntimeException e) {
                    log.warn("Falha ao enviar alerta do prazo {}: {}", alert.deadlineId(), e.getMessage());
                }
            }
            return sent;
        } finally {
            port.releaseAlertLock();
        }
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(BUSINESS_ZONE));
    }
}
