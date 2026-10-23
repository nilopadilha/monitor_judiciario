package br.com.monitorjudicial.monitoring.infrastructure.persistence;

import br.com.monitorjudicial.monitoring.domain.CandidatePublication;
import br.com.monitorjudicial.monitoring.domain.port.CourtMovementSource;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

/** Placeholder até o adapter MNI/PJe da fase 2 (ADR-0007). */
@Component
class NoopCourtMovementSource implements CourtMovementSource {

    @Override
    public List<CandidatePublication> movementsOf(String processNumber, String courtCode, LocalDate from,
            LocalDate to) {
        return List.of();
    }
}
