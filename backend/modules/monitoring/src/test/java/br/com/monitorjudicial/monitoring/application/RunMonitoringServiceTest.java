package br.com.monitorjudicial.monitoring.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.monitorjudicial.monitoring.domain.ExecutionStatus;
import br.com.monitorjudicial.monitoring.domain.MonitoringExecution;
import br.com.monitorjudicial.monitoring.domain.port.MonitoringExecutionRepository;
import br.com.monitorjudicial.monitoring.domain.port.MonitoringLock;
import br.com.monitorjudicial.processes.api.MonitoredProcess;
import br.com.monitorjudicial.processes.api.ProcessDirectory;
import br.com.monitorjudicial.shared.audit.Auditor;
import br.com.monitorjudicial.shared.domain.ConflictException;
import br.com.monitorjudicial.shared.security.CurrentUserProvider;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RunMonitoringServiceTest {

    private final MonitoringLock lock = mock(MonitoringLock.class);
    private final MonitoringExecutionRepository executions = mock(MonitoringExecutionRepository.class);
    private final ProcessDirectory processes = mock(ProcessDirectory.class);
    private final ProcessChecker checker = mock(ProcessChecker.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-23T09:00:00Z"), ZoneOffset.UTC);
    private final MonitoringProperties properties = new MonitoringProperties(3, 30, 1, ZoneId.of("America/Sao_Paulo"));
    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
    private final CurrentUserProvider currentUser = Optional::empty;
    private final RunMonitoringService service = new RunMonitoringService(lock, executions, processes, checker,
            properties, currentUser, mock(Auditor.class), Runnable::run, meters, clock);

    @Test
    void runsAllProcessesWithBoundedConcurrencyAndReleasesLock() {
        AtomicBoolean released = new AtomicBoolean();
        when(lock.tryAcquire()).thenReturn(Optional.of(() -> released.set(true)));
        when(executions.save(any())).thenAnswer(i -> i.getArgument(0));
        List<MonitoredProcess> targets = IntStream.range(0, 12)
                .mapToObj(i -> Fixtures.process(null, clock.instant())).toList();
        when(processes.findMonitorable()).thenReturn(targets);
        AtomicInteger inFlight = new AtomicInteger();
        AtomicInteger maxInFlight = new AtomicInteger();
        when(checker.check(any(), any(), any())).thenAnswer(i -> {
            maxInFlight.accumulateAndGet(inFlight.incrementAndGet(), Math::max);
            Thread.sleep(20);
            inFlight.decrementAndGet();
            MonitoredProcess p = i.getArgument(1);
            return p == targets.getFirst() ? CheckOutcome.failure() : CheckOutcome.success(1);
        });

        var view = service.startManual();

        assertThat(view.status()).isEqualTo(ExecutionStatus.RUNNING);
        var captor = ArgumentCaptor.forClass(MonitoringExecution.class);
        verify(executions, org.mockito.Mockito.atLeast(2)).save(captor.capture());
        MonitoringExecution finished = captor.getValue();
        assertThat(finished.status()).isEqualTo(ExecutionStatus.COMPLETED_WITH_FAILURES);
        assertThat(finished.processesChecked()).isEqualTo(12);
        assertThat(finished.newPublications()).isEqualTo(11);
        assertThat(finished.failedJobs()).isEqualTo(1);
        assertThat(maxInFlight.get()).isLessThanOrEqualTo(3);
        assertThat(released).isTrue();
        assertThat(meters.counter("monitoring.publications.new").count()).isEqualTo(11);
        verify(executions).abortRunning(clock.instant());
    }

    @Test
    void manualStartConflictsWhenLocked() {
        when(lock.tryAcquire()).thenReturn(Optional.empty());
        assertThatThrownBy(service::startManual).isInstanceOf(ConflictException.class);
        assertThat(service.startScheduled()).isEmpty();
    }

    @Test
    void unexpectedErrorMarksExecutionFailed() {
        when(lock.tryAcquire()).thenReturn(Optional.of(() -> { }));
        when(executions.save(any())).thenAnswer(i -> i.getArgument(0));
        when(processes.findMonitorable()).thenThrow(new IllegalStateException("banco fora"));

        assertThat(service.startScheduled()).isPresent();
        var captor = ArgumentCaptor.forClass(MonitoringExecution.class);
        verify(executions, org.mockito.Mockito.atLeast(2)).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(ExecutionStatus.FAILED);
        assertThat(captor.getValue().errorMessage()).contains("banco fora");
    }

    @Test
    void lockIsReleasedIfStartupFails() {
        AtomicBoolean released = new AtomicBoolean();
        when(lock.tryAcquire()).thenReturn(Optional.of(() -> released.set(true)));
        when(executions.abortRunning(any())).thenThrow(new IllegalStateException("x"));
        assertThatThrownBy(service::startScheduled).isInstanceOf(IllegalStateException.class);
        assertThat(released).isTrue();
        verify(processes, org.mockito.Mockito.never()).findMonitorable();
        verify(executions, org.mockito.Mockito.never()).save(eq(null));
    }
}
