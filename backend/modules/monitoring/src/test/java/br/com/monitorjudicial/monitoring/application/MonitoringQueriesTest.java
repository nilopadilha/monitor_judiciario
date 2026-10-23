package br.com.monitorjudicial.monitoring.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.monitorjudicial.monitoring.domain.ComparisonResult;
import br.com.monitorjudicial.monitoring.domain.ExecutionTrigger;
import br.com.monitorjudicial.monitoring.domain.MonitoringExecution;
import br.com.monitorjudicial.monitoring.domain.MonitoringJob;
import br.com.monitorjudicial.monitoring.domain.Movement;
import br.com.monitorjudicial.monitoring.domain.MovementType;
import br.com.monitorjudicial.monitoring.domain.Publication;
import br.com.monitorjudicial.monitoring.domain.PublicationComparisonService;
import br.com.monitorjudicial.monitoring.domain.PublicationHistory;
import br.com.monitorjudicial.monitoring.domain.port.MonitoringExecutionRepository;
import br.com.monitorjudicial.monitoring.domain.port.MonitoringJobRepository;
import br.com.monitorjudicial.monitoring.domain.port.MovementRepository;
import br.com.monitorjudicial.monitoring.domain.port.PublicationRepository;
import br.com.monitorjudicial.monitoring.domain.port.PublicationRepository.PublicationFilter;
import br.com.monitorjudicial.processes.api.ProcessDirectory;
import br.com.monitorjudicial.processes.api.ProcessSummary;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import br.com.monitorjudicial.shared.audit.Auditor;
import br.com.monitorjudicial.shared.domain.ResourceNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MonitoringQueriesTest {

    private final PublicationRepository publications = mock(PublicationRepository.class);
    private final MovementRepository movements = mock(MovementRepository.class);
    private final MonitoringExecutionRepository executions = mock(MonitoringExecutionRepository.class);
    private final MonitoringJobRepository jobs = mock(MonitoringJobRepository.class);
    private final ProcessDirectory processes = mock(ProcessDirectory.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-23T09:00:00Z"), ZoneOffset.UTC);

    private final UUID clientId = UUID.randomUUID();
    private final UUID processId = UUID.randomUUID();
    private final ProcessSummary summary = new ProcessSummary(processId, clientId, "1001234-83.2026.8.26.0100", "TJSP",
            null);

    private Publication publication() {
        var fresh = (ComparisonResult.New) new PublicationComparisonService().compare(
                MonitoringDomainTestSupport.candidate("h"), PublicationHistory.empty());
        return Publication.fromNew(fresh, processId, clientId, clock.instant());
    }

    @Test
    void publicationsAreScopedAndMarkedRead() {
        Publication p = publication();
        var service = new PublicationService(publications, processes, mock(Auditor.class), clock);
        when(publications.search(any(), any())).thenReturn(PageResult.of(List.of(p), 0, 20, 1));
        when(processes.findByIds(anyCollection())).thenReturn(Map.of(processId, summary));
        when(publications.findById(p.id())).thenReturn(Optional.of(p));
        when(processes.findById(processId)).thenReturn(Optional.of(summary));

        var page = service.search(null, null, null, true, PageQuery.of(0, 20), Fixtures.customer(clientId));
        var filter = ArgumentCaptor.forClass(PublicationFilter.class);
        verify(publications).search(filter.capture(), any());
        assertThat(filter.getValue().clientId()).isEqualTo(clientId);
        assertThat(page.content().getFirst().processNumber()).isEqualTo(summary.formattedNumber());

        assertThat(service.get(p.id(), Fixtures.manager()).read()).isFalse();
        assertThat(service.markRead(p.id(), Fixtures.manager()).read()).isTrue();
        assertThatThrownBy(() -> service.get(p.id(), Fixtures.customer(UUID.randomUUID())))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void movementsRespectScope() {
        var service = new MovementQueryService(movements, processes);
        when(processes.findById(processId)).thenReturn(Optional.of(summary));
        Movement m = Movement.fromPublication(publication(), MovementType.DECISAO, clock.instant());
        when(movements.findByProcess(processId, PageQuery.of(0, 50))).thenReturn(PageResult.of(List.of(m), 0, 50, 1));

        assertThat(service.list(processId, PageQuery.of(0, 50), Fixtures.manager()).content())
                .singleElement().extracting(MovementView::typeLabel).isEqualTo("Decisão");
        assertThatThrownBy(() -> service.list(processId, PageQuery.of(0, 50), Fixtures.customer(UUID.randomUUID())))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void executionsWithJobDetails() {
        var service = new MonitoringQueryService(executions, jobs, processes);
        MonitoringExecution e = MonitoringExecution.start(ExecutionTrigger.MANUAL, "a", clock.instant());
        when(executions.findById(e.id())).thenReturn(Optional.of(e));
        when(executions.findRecent(any())).thenReturn(PageResult.of(List.of(e), 0, 20, 1));
        when(jobs.findByExecution(e.id())).thenReturn(List.of(
                MonitoringJob.success(e.id(), processId, LocalDate.now(), 10, 1, clock.instant()),
                MonitoringJob.failure(e.id(), UUID.randomUUID(), LocalDate.now(), 10, "erro", clock.instant())));
        when(processes.findByIds(anyCollection())).thenReturn(Map.of(processId, summary));

        var view = service.get(e.id());
        assertThat(view.jobs()).hasSize(2);
        assertThat(view.jobs().get(0).processNumber()).isEqualTo(summary.formattedNumber());
        assertThat(view.jobs().get(1).processNumber()).isNull();
        assertThat(service.list(PageQuery.of(0, 20)).content()).hasSize(1);
        assertThatThrownBy(() -> service.get(UUID.randomUUID())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void insightsCombineUnreadAndLastJob() {
        var adapter = new ProcessMonitoringInsightsAdapter(publications, jobs);
        UUID failing = UUID.randomUUID();
        when(publications.countUnreadByProcess(anyCollection())).thenReturn(Map.of(processId, 3L));
        when(jobs.lastJobByProcess(anyCollection())).thenReturn(Map.of(failing,
                MonitoringJob.failure(UUID.randomUUID(), failing, LocalDate.now(), 1, "DJEN fora", clock.instant())));
        when(publications.processIdsWithUnread()).thenReturn(Set.of(processId));
        when(jobs.processIdsWithFailedLastJob()).thenReturn(Set.of(failing));

        var status = adapter.statusOf(List.of(processId, failing));
        assertThat(status.get(processId).unreadPublications()).isEqualTo(3);
        assertThat(status.get(failing).lastCheckFailed()).isTrue();
        assertThat(status.get(failing).lastError()).isEqualTo("DJEN fora");
        assertThat(adapter.statusOf(List.of())).isEmpty();
        assertThat(adapter.processesWithUnreadPublications()).containsExactly(processId);
        assertThat(adapter.processesWithFailedLastCheck()).containsExactly(failing);
    }
}
