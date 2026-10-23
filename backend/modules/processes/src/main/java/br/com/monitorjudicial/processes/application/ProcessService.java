package br.com.monitorjudicial.processes.application;

import br.com.monitorjudicial.clients.api.ClientDirectory;
import br.com.monitorjudicial.clients.api.ClientSummary;
import br.com.monitorjudicial.processes.api.MonitoredProcess;
import br.com.monitorjudicial.processes.api.ProcessDirectory;
import br.com.monitorjudicial.processes.api.ProcessMonitoringInsights;
import br.com.monitorjudicial.processes.api.ProcessMonitoringStatus;
import br.com.monitorjudicial.processes.api.ProcessSummary;
import br.com.monitorjudicial.processes.domain.Process;
import br.com.monitorjudicial.processes.domain.ProcessNumber;
import br.com.monitorjudicial.processes.domain.ProcessStatus;
import br.com.monitorjudicial.processes.domain.port.ProcessRepository;
import br.com.monitorjudicial.processes.domain.port.ProcessRepository.ProcessFilter;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import br.com.monitorjudicial.shared.audit.Auditor;
import br.com.monitorjudicial.shared.domain.BusinessRuleViolationException;
import br.com.monitorjudicial.shared.domain.ConflictException;
import br.com.monitorjudicial.shared.domain.ResourceNotFoundException;
import br.com.monitorjudicial.shared.security.CurrentUser;
import br.com.monitorjudicial.tribunals.api.TribunalDirectory;
import br.com.monitorjudicial.tribunals.api.TribunalSummary;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** UC04 — cadastro de processos; também implementa o contrato público {@link ProcessDirectory}. */
@Service
public class ProcessService implements ProcessDirectory {

    private static final String RESOURCE = "process";

    private final ProcessRepository processes;
    private final ClientDirectory clients;
    private final TribunalDirectory tribunals;
    private final ProcessMonitoringInsights insights;
    private final Auditor auditor;
    private final Clock clock;

    public ProcessService(ProcessRepository processes, ClientDirectory clients, TribunalDirectory tribunals,
            ProcessMonitoringInsights insights, Auditor auditor, Clock clock) {
        this.processes = processes;
        this.clients = clients;
        this.tribunals = tribunals;
        this.insights = insights;
        this.auditor = auditor;
        this.clock = clock;
    }

    @Transactional
    public ProcessView register(RegisterProcessCommand command) {
        ProcessNumber number = ProcessNumber.parse(command.number());
        String courtCode = number.courtCode();
        ClientSummary client = clients.findById(command.clientId())
                .orElseThrow(() -> new BusinessRuleViolationException("client-not-found",
                        "Cliente " + command.clientId() + " não existe."));
        if (!client.active()) {
            throw new BusinessRuleViolationException("client-inactive",
                    "Não é possível cadastrar processos para cliente inativo.");
        }
        boolean courtActive = tribunals.findByCode(courtCode).map(TribunalSummary::active).orElse(false);
        if (!courtActive) {
            throw new BusinessRuleViolationException("court-not-monitored",
                    "O tribunal " + courtCode + " não está habilitado para monitoramento.");
        }
        if (processes.existsByNumber(number)) {
            throw new ConflictException("process-already-registered",
                    "O processo " + number.formatted() + " já está cadastrado.");
        }
        Process process = processes.save(Process.register(client.id(), number, command.subject(), clock.instant()));
        auditor.record("CREATE", RESOURCE, process.id(), Map.of("number", number.formatted(), "court", courtCode));
        return toView(process, client.name(), ProcessMonitoringStatus.NONE);
    }

    @Transactional
    public ProcessView update(UUID id, String subject, ProcessStatus status) {
        Process process = load(id);
        process.update(subject, status, clock.instant());
        processes.save(process);
        auditor.record("UPDATE", RESOURCE, id, Map.of("status", status.name()));
        return enrich(List.of(process)).getFirst();
    }

    @Transactional
    public void archive(UUID id) {
        Process process = load(id);
        process.archive(clock.instant());
        processes.save(process);
        auditor.record("ARCHIVE", RESOURCE, id, Map.of());
    }

    @Transactional(readOnly = true)
    public ProcessView get(UUID id, CurrentUser user) {
        Process process = load(id);
        if (user.dataScope().filter(scope -> !scope.equals(process.clientId())).isPresent()) {
            throw new ResourceNotFoundException("processo", id);
        }
        return enrich(List.of(process)).getFirst();
    }

    @Transactional(readOnly = true)
    public PageResult<ProcessView> search(ProcessSearch search, PageQuery page, CurrentUser user) {
        UUID clientId = user.dataScope().orElse(search.clientId());
        Collection<UUID> restrictTo = switch (search.situation()) {
            case null -> null;
            case NEW_PUBLICATION -> insights.processesWithUnreadPublications();
            case FAILED -> insights.processesWithFailedLastCheck();
            default -> null;
        };
        ProcessStatus status = search.status();
        if (search.situation() == MonitoringSituation.PAUSED) {
            status = ProcessStatus.PAUSED;
        } else if (search.situation() == MonitoringSituation.ARCHIVED) {
            status = ProcessStatus.ARCHIVED;
        }
        PageResult<Process> result = processes.search(
                new ProcessFilter(clientId, status, search.courtCode(), search.number(), restrictTo), page);
        List<ProcessView> views = enrich(result.content());
        return new PageResult<>(views, result.page(), result.size(), result.totalElements(), result.totalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonitoredProcess> findMonitorable() {
        return processes.findByStatusOrderByLastCheckedAt(ProcessStatus.ACTIVE).stream()
                .map(p -> new MonitoredProcess(p.id(), p.clientId(), p.number().digits(), p.number().formatted(),
                        p.courtCode(), p.lastCheckedAt().orElse(null), p.createdAt()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProcessSummary> findById(UUID id) {
        return processes.findById(id).map(ProcessService::summary);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, ProcessSummary> findByIds(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return processes.findAllById(ids).stream().map(ProcessService::summary)
                .collect(Collectors.toMap(ProcessSummary::id, Function.identity()));
    }

    @Override
    @Transactional
    public void markChecked(UUID processId, Instant checkedAt) {
        processes.updateLastCheckedAt(processId, checkedAt);
    }

    private List<ProcessView> enrich(List<Process> page) {
        if (page.isEmpty()) {
            return List.of();
        }
        Set<UUID> clientIds = page.stream().map(Process::clientId).collect(Collectors.toSet());
        Map<UUID, ClientSummary> clientsById = clients.findByIds(clientIds);
        Map<UUID, ProcessMonitoringStatus> status = insights.statusOf(page.stream().map(Process::id).toList());
        return page.stream()
                .map(p -> toView(p,
                        Optional.ofNullable(clientsById.get(p.clientId())).map(ClientSummary::name).orElse(null),
                        status.getOrDefault(p.id(), ProcessMonitoringStatus.NONE)))
                .toList();
    }

    static MonitoringSituation situationOf(ProcessStatus status, ProcessMonitoringStatus monitoring) {
        return switch (status) {
            case ARCHIVED -> MonitoringSituation.ARCHIVED;
            case PAUSED -> MonitoringSituation.PAUSED;
            case ACTIVE -> monitoring.lastCheckFailed() ? MonitoringSituation.FAILED
                    : monitoring.unreadPublications() > 0 ? MonitoringSituation.NEW_PUBLICATION
                    : MonitoringSituation.MONITORED;
        };
    }

    private static ProcessView toView(Process p, String clientName, ProcessMonitoringStatus monitoring) {
        return new ProcessView(p.id(), p.clientId(), clientName, p.number().formatted(), p.courtCode(),
                p.subject().orElse(null), p.status(), situationOf(p.status(), monitoring),
                monitoring.unreadPublications(), monitoring.lastError(), p.lastCheckedAt().orElse(null),
                p.createdAt());
    }

    private Process load(UUID id) {
        return processes.findById(id).orElseThrow(() -> new ResourceNotFoundException("processo", id));
    }

    private static ProcessSummary summary(Process p) {
        return new ProcessSummary(p.id(), p.clientId(), p.number().formatted(), p.courtCode(),
                p.subject().orElse(null));
    }
}
