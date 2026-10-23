package br.com.monitorjudicial.monitoring.application;

import br.com.monitorjudicial.djen.api.DjenClient;
import br.com.monitorjudicial.djen.api.DjenCommunication;
import br.com.monitorjudicial.djen.api.DjenSearch;
import br.com.monitorjudicial.monitoring.domain.CandidatePublication;
import br.com.monitorjudicial.monitoring.domain.PublicationSource;
import br.com.monitorjudicial.monitoring.domain.port.CourtMovementSource;
import br.com.monitorjudicial.processes.api.MonitoredProcess;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Verifica um processo: consulta as fontes fora de transação e delega a gravação ao {@link CheckResultRecorder}. */
@Component
class ProcessChecker {

    private static final Logger log = LoggerFactory.getLogger(ProcessChecker.class);

    private final DjenClient djen;
    private final CourtMovementSource courtSource;
    private final CheckResultRecorder recorder;
    private final MonitoringProperties properties;

    ProcessChecker(DjenClient djen, CourtMovementSource courtSource, CheckResultRecorder recorder,
            MonitoringProperties properties) {
        this.djen = djen;
        this.courtSource = courtSource;
        this.recorder = recorder;
        this.properties = properties;
    }

    CheckOutcome check(UUID executionId, MonitoredProcess process, LocalDate today) {
        long start = System.nanoTime();
        CheckWindow window = CheckWindow.of(process, today, properties.zone(), properties.initialLookbackDays(),
                properties.overlapDays());
        try {
            List<CandidatePublication> candidates = new ArrayList<>();
            djen.searchCommunications(new DjenSearch(process.number(), process.courtCode(), window.from(), window.to()))
                    .stream()
                    .filter(c -> c.processNumber() == null || c.processNumber().equals(process.number()))
                    .map(ProcessChecker::toCandidate)
                    .forEach(candidates::add);
            candidates.addAll(courtSource.movementsOf(process.number(), process.courtCode(), window.from(),
                    window.to()));
            int created = recorder.recordSuccess(executionId, process, candidates, today, elapsedMs(start));
            return CheckOutcome.success(created);
        } catch (RuntimeException e) {
            log.warn("Falha ao verificar o processo {}: {}", process.formattedNumber(), e.getMessage());
            recorder.recordFailure(executionId, process, today, elapsedMs(start), e.getMessage());
            return CheckOutcome.failure();
        }
    }

    static CandidatePublication toCandidate(DjenCommunication c) {
        return new CandidatePublication(c.hash(), c.djenId(), c.availableAt(), c.communicationType(),
                c.documentType(), c.organName(), c.text(), c.link(), PublicationSource.DJEN);
    }

    private static long elapsedMs(long start) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    }
}
