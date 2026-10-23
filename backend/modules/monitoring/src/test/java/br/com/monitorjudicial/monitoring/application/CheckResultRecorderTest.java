package br.com.monitorjudicial.monitoring.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.monitorjudicial.monitoring.api.MonitoringEvents;
import br.com.monitorjudicial.monitoring.api.ProcessEvolutionDetected;
import br.com.monitorjudicial.monitoring.domain.CandidatePublication;
import br.com.monitorjudicial.monitoring.domain.JobStatus;
import br.com.monitorjudicial.monitoring.domain.MonitoringJob;
import br.com.monitorjudicial.monitoring.domain.Movement;
import br.com.monitorjudicial.monitoring.domain.MovementClassifier;
import br.com.monitorjudicial.monitoring.domain.MovementType;
import br.com.monitorjudicial.monitoring.domain.OutboxEvent;
import br.com.monitorjudicial.monitoring.domain.PublicationComparisonService;
import br.com.monitorjudicial.monitoring.domain.PublicationHistory;
import br.com.monitorjudicial.monitoring.domain.PublicationSource;
import br.com.monitorjudicial.monitoring.domain.port.MonitoringJobRepository;
import br.com.monitorjudicial.monitoring.domain.port.MovementRepository;
import br.com.monitorjudicial.monitoring.domain.port.OutboxRepository;
import br.com.monitorjudicial.monitoring.domain.port.PublicationRepository;
import br.com.monitorjudicial.processes.api.ProcessDirectory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CheckResultRecorderTest {

    private final PublicationRepository publications = mock(PublicationRepository.class);
    private final MovementRepository movements = mock(MovementRepository.class);
    private final MonitoringJobRepository jobs = mock(MonitoringJobRepository.class);
    private final OutboxRepository outbox = mock(OutboxRepository.class);
    private final ProcessDirectory processes = mock(ProcessDirectory.class);
    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-23T09:00:00Z"), ZoneOffset.UTC);
    private final CheckResultRecorder recorder = new CheckResultRecorder(publications, movements, jobs, outbox,
            processes, new PublicationComparisonService(), new MovementClassifier(), json, clock);

    private static CandidatePublication candidate(String hash, String type, String text) {
        return new CandidatePublication(hash, 1L, LocalDate.of(2026, 10, 22), "Intimação", type, "Vara", text, null,
                PublicationSource.DJEN);
    }

    @Test
    void recordsOnlyNoveltiesWithMovementOutboxAndJobInOneUnit() throws Exception {
        var process = Fixtures.process(null, clock.instant());
        when(publications.historyOf(process.id())).thenReturn(PublicationHistory.empty());
        UUID execution = UUID.randomUUID();

        int created = recorder.recordSuccess(execution, process, List.of(
                candidate("h1", "Sentença", "Julgo procedente."),
                candidate("h1", "Sentença", "Julgo procedente."),
                candidate("h2", "Sentença", "JULGO procedente."),
                candidate("h3", "Despacho", "Cite-se.")), LocalDate.of(2026, 10, 23), 42);

        assertThat(created).isEqualTo(2);
        verify(publications, times(2)).save(any());
        var movementCaptor = ArgumentCaptor.forClass(Movement.class);
        verify(movements, times(2)).save(movementCaptor.capture());
        assertThat(movementCaptor.getAllValues()).extracting(Movement::type)
                .containsExactly(MovementType.SENTENCA, MovementType.DESPACHO);

        var eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outbox, times(2)).append(eventCaptor.capture());
        OutboxEvent event = eventCaptor.getAllValues().getFirst();
        assertThat(event.eventType()).isEqualTo(MonitoringEvents.PROCESS_EVOLUTION_DETECTED);
        var payload = json.readValue(event.payload(), ProcessEvolutionDetected.class);
        assertThat(payload.eventId()).isEqualTo(event.id());
        assertThat(payload.movementLabel()).isEqualTo("Sentença");
        assertThat(payload.processNumber()).isEqualTo("1001234-83.2026.8.26.0100");

        verify(processes).markChecked(process.id(), clock.instant());
        var jobCaptor = ArgumentCaptor.forClass(MonitoringJob.class);
        verify(jobs).save(jobCaptor.capture());
        assertThat(jobCaptor.getValue().status()).isEqualTo(JobStatus.SUCCESS);
        assertThat(jobCaptor.getValue().newPublications()).isEqualTo(2);
    }

    @Test
    void recordsFailure() {
        var process = Fixtures.process(null, clock.instant());
        recorder.recordFailure(UUID.randomUUID(), process, LocalDate.of(2026, 10, 23), 5, "timeout");
        var jobCaptor = ArgumentCaptor.forClass(MonitoringJob.class);
        verify(jobs).save(jobCaptor.capture());
        assertThat(jobCaptor.getValue().status()).isEqualTo(JobStatus.FAILED);
        assertThat(jobCaptor.getValue().errorMessage()).isEqualTo("timeout");
    }
}
