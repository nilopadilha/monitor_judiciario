package br.com.monitorjudicial.monitoring.application;

import br.com.monitorjudicial.monitoring.domain.ExecutionTrigger;
import br.com.monitorjudicial.monitoring.domain.MonitoringExecution;
import br.com.monitorjudicial.monitoring.domain.port.MonitoringExecutionRepository;
import br.com.monitorjudicial.monitoring.domain.port.MonitoringLock;
import br.com.monitorjudicial.processes.api.MonitoredProcess;
import br.com.monitorjudicial.processes.api.ProcessDirectory;
import br.com.monitorjudicial.shared.audit.Auditor;
import br.com.monitorjudicial.shared.domain.ConflictException;
import br.com.monitorjudicial.shared.security.CurrentUser;
import br.com.monitorjudicial.shared.security.CurrentUserProvider;
import br.com.monitorjudicial.shared.observability.CorrelationId;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * UC05 — executa o monitoramento: adquire o lock, cria a execução e verifica cada processo ativo em uma Virtual
 * Thread, com concorrência limitada por semáforo (ADR-0005, ADR-0006).
 */
@Service
public class RunMonitoringService {

    private static final Logger log = LoggerFactory.getLogger(RunMonitoringService.class);

    private final MonitoringLock lock;
    private final MonitoringExecutionRepository executions;
    private final ProcessDirectory processes;
    private final ProcessChecker checker;
    private final MonitoringProperties properties;
    private final CurrentUserProvider currentUser;
    private final Auditor auditor;
    private final Executor launcher;
    private final MeterRegistry meters;
    private final Clock clock;

    public RunMonitoringService(MonitoringLock lock, MonitoringExecutionRepository executions,
            ProcessDirectory processes, ProcessChecker checker, MonitoringProperties properties,
            CurrentUserProvider currentUser, Auditor auditor, @Qualifier("monitoringLauncher") Executor launcher,
            MeterRegistry meters, Clock clock) {
        this.lock = lock;
        this.executions = executions;
        this.processes = processes;
        this.checker = checker;
        this.properties = properties;
        this.currentUser = currentUser;
        this.auditor = auditor;
        this.launcher = launcher;
        this.meters = meters;
        this.clock = clock;
    }

    /** Disparo manual: 409 se já houver execução em andamento. A execução prossegue em segundo plano. */
    public MonitoringExecutionView startManual() {
        String actor = currentUser.find().map(CurrentUser::email).orElse(Auditor.SYSTEM_ACTOR);
        MonitoringExecutionView execution = start(ExecutionTrigger.MANUAL, actor)
                .orElseThrow(() -> new ConflictException("monitoring-already-running",
                        "Já existe uma execução de monitoramento em andamento."));
        auditor.record("RUN", "monitoring-execution", execution.id(), Map.of("trigger", "MANUAL"));
        return execution;
    }

    /** Disparo agendado: se outra réplica já estiver executando, apenas registra e retorna. */
    public Optional<UUID> startScheduled() {
        Optional<MonitoringExecutionView> execution = start(ExecutionTrigger.SCHEDULED, Auditor.SYSTEM_ACTOR);
        if (execution.isEmpty()) {
            log.info("Monitoramento agendado ignorado: outra execução detém o lock");
        }
        return execution.map(MonitoringExecutionView::id);
    }

    /** Retorna o retrato da execução tirado antes do disparo: o objeto passa a ser alterado pela thread de execução. */
    private Optional<MonitoringExecutionView> start(ExecutionTrigger trigger, String actor) {
        Optional<MonitoringLock.Handle> handle = lock.tryAcquire();
        if (handle.isEmpty()) {
            return Optional.empty();
        }
        try {
            int aborted = executions.abortRunning(clock.instant());
            if (aborted > 0) {
                log.warn("{} execução(ões) órfã(s) marcada(s) como ABORTED", aborted);
            }
            MonitoringExecution execution = executions.save(MonitoringExecution.start(trigger, actor, clock.instant()));
            MonitoringExecutionView snapshot = MonitoringExecutionView.of(execution, List.of());
            String correlationId = CorrelationId.current().orElse(execution.id().toString());
            launcher.execute(() -> {
                try (MonitoringLock.Handle held = handle.get()) {
                    MDC.put(CorrelationId.MDC_KEY, correlationId);
                    run(execution);
                } finally {
                    MDC.remove(CorrelationId.MDC_KEY);
                }
            });
            return Optional.of(snapshot);
        } catch (RuntimeException e) {
            handle.get().close();
            throw e;
        }
    }

    void run(MonitoringExecution execution) {
        Timer.Sample sample = Timer.start(meters);
        try {
            List<MonitoredProcess> targets = processes.findMonitorable();
            LocalDate today = LocalDate.now(clock.withZone(properties.zone()));
            String correlationId = MDC.get(CorrelationId.MDC_KEY);
            Semaphore permits = new Semaphore(properties.maxConcurrency());
            List<Future<CheckOutcome>> futures = new ArrayList<>(targets.size());
            try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
                for (MonitoredProcess process : targets) {
                    futures.add(pool.submit(() -> {
                        permits.acquire();
                        try {
                            if (correlationId != null) {
                                MDC.put(CorrelationId.MDC_KEY, correlationId);
                            }
                            return checker.check(execution.id(), process, today);
                        } finally {
                            permits.release();
                            MDC.remove(CorrelationId.MDC_KEY);
                        }
                    }));
                }
            }
            int created = 0;
            int failed = 0;
            for (Future<CheckOutcome> future : futures) {
                CheckOutcome outcome = future.get();
                created += outcome.newPublications();
                failed += outcome.success() ? 0 : 1;
            }
            execution.complete(targets.size(), created, failed, clock.instant());
            meters.counter("monitoring.publications.new").increment(created);
            log.info("Execução {} concluída: {} processos, {} publicações novas, {} falhas", execution.id(),
                    targets.size(), created, failed);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            execution.fail("Execução interrompida", clock.instant());
        } catch (ExecutionException | RuntimeException e) {
            log.error("Execução {} falhou", execution.id(), e);
            execution.fail(e.getMessage(), clock.instant());
        } finally {
            executions.save(execution);
            sample.stop(meters.timer("monitoring.execution.duration", "status", execution.status().name()));
        }
    }
}
