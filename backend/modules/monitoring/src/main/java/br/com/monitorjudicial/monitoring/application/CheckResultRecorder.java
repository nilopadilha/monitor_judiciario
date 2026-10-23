package br.com.monitorjudicial.monitoring.application;

import br.com.monitorjudicial.monitoring.api.MonitoringEvents;
import br.com.monitorjudicial.monitoring.api.ProcessEvolutionDetected;
import br.com.monitorjudicial.monitoring.domain.CandidatePublication;
import br.com.monitorjudicial.monitoring.domain.ComparisonResult;
import br.com.monitorjudicial.monitoring.domain.MonitoringJob;
import br.com.monitorjudicial.monitoring.domain.Movement;
import br.com.monitorjudicial.monitoring.domain.MovementClassifier;
import br.com.monitorjudicial.monitoring.domain.MovementType;
import br.com.monitorjudicial.monitoring.domain.OutboxEvent;
import br.com.monitorjudicial.monitoring.domain.Publication;
import br.com.monitorjudicial.monitoring.domain.PublicationComparisonService;
import br.com.monitorjudicial.monitoring.domain.PublicationHistory;
import br.com.monitorjudicial.monitoring.domain.port.MonitoringJobRepository;
import br.com.monitorjudicial.monitoring.domain.port.MovementRepository;
import br.com.monitorjudicial.monitoring.domain.port.OutboxRepository;
import br.com.monitorjudicial.monitoring.domain.port.PublicationRepository;
import br.com.monitorjudicial.processes.api.MonitoredProcess;
import br.com.monitorjudicial.processes.api.ProcessDirectory;
import br.com.monitorjudicial.shared.domain.Ids;
import br.com.monitorjudicial.shared.observability.CorrelationId;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Grava, numa única transação por processo, as publicações novas, as movimentações, os eventos de outbox, o job e a
 * data da verificação (fases 4 a 7 do PDF).
 */
@Component
class CheckResultRecorder {

    private static final Logger log = LoggerFactory.getLogger(CheckResultRecorder.class);
    static final int EXCERPT_LENGTH = 280;

    private final PublicationRepository publications;
    private final MovementRepository movements;
    private final MonitoringJobRepository jobs;
    private final OutboxRepository outbox;
    private final ProcessDirectory processes;
    private final PublicationComparisonService comparison;
    private final MovementClassifier classifier;
    private final ObjectMapper json;
    private final Clock clock;

    CheckResultRecorder(PublicationRepository publications, MovementRepository movements, MonitoringJobRepository jobs,
            OutboxRepository outbox, ProcessDirectory processes, PublicationComparisonService comparison,
            MovementClassifier classifier, ObjectMapper json, Clock clock) {
        this.publications = publications;
        this.movements = movements;
        this.jobs = jobs;
        this.outbox = outbox;
        this.processes = processes;
        this.comparison = comparison;
        this.classifier = classifier;
        this.json = json;
        this.clock = clock;
    }

    @Transactional
    public int recordSuccess(UUID executionId, MonitoredProcess process, List<CandidatePublication> candidates,
            LocalDate date, long elapsedMs) {
        Instant now = clock.instant();
        PublicationHistory history = publications.historyOf(process.id());
        int created = 0;
        for (CandidatePublication candidate : candidates) {
            switch (comparison.compare(candidate, history)) {
                case ComparisonResult.New fresh -> {
                    Publication publication = Publication.fromNew(fresh, process.id(), process.clientId(), now);
                    publications.save(publication);
                    MovementType type = classifier.classify(candidate.documentType(), candidate.communicationType(),
                            candidate.content());
                    movements.save(Movement.fromPublication(publication, type, now));
                    outbox.append(evolutionEvent(process, publication, type, now));
                    history.accept(publication);
                    created++;
                }
                case ComparisonResult.Republication r -> log.debug(
                        "Republicação ignorada no processo {}: hash {} repete o conteúdo da publicação {}",
                        process.formattedNumber(), r.hash(), r.originalPublicationId());
                case ComparisonResult.Duplicate d -> { }
            }
        }
        processes.markChecked(process.id(), now);
        jobs.save(MonitoringJob.success(executionId, process.id(), date, elapsedMs, created, now));
        return created;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(UUID executionId, MonitoredProcess process, LocalDate date, long elapsedMs,
            String error) {
        jobs.save(MonitoringJob.failure(executionId, process.id(), date, elapsedMs, error, clock.instant()));
    }

    private OutboxEvent evolutionEvent(MonitoredProcess process, Publication publication, MovementType type,
            Instant now) {
        UUID eventId = Ids.newId();
        var event = new ProcessEvolutionDetected(eventId, process.id(), process.clientId(), process.formattedNumber(),
                process.courtCode(), publication.id(), publication.publicationDate(), type.name(), type.label(),
                publication.excerpt(EXCERPT_LENGTH), now);
        try {
            return new OutboxEvent(eventId, "process", process.id(), MonitoringEvents.PROCESS_EVOLUTION_DETECTED,
                    json.writeValueAsString(event), CorrelationId.current().orElse(null), now);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar evento", e);
        }
    }
}
