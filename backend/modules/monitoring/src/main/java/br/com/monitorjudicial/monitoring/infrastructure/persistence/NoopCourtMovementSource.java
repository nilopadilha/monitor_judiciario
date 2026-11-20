package br.com.monitorjudicial.monitoring.infrastructure.persistence;

import br.com.monitorjudicial.monitoring.domain.CandidatePublication;
import br.com.monitorjudicial.monitoring.domain.port.CourtMovementSource;
import java.time.LocalDate;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Placeholder quando DataJud/MNI está desabilitado (ADR-0007). */
@Component
@ConditionalOnProperty(name = "datajud.enabled", havingValue = "false", matchIfMissing = true)
class NoopCourtMovementSource implements CourtMovementSource {

    @Override
    public List<CandidatePublication> movementsOf(String processNumber, String courtCode, LocalDate from,
            LocalDate to) {
        return List.of();
    }
}
