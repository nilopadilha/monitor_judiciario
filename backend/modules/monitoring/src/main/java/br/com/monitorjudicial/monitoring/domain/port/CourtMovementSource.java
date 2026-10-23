package br.com.monitorjudicial.monitoring.domain.port;

import br.com.monitorjudicial.monitoring.domain.CandidatePublication;
import java.time.LocalDate;
import java.util.List;

/**
 * Fonte de tramitações do tribunal (PJe via MNI). Sem implementação real no MVP (ADR-0007); o adapter da fase 2
 * devolverá candidatas com {@code source = PJE}.
 */
public interface CourtMovementSource {

    List<CandidatePublication> movementsOf(String processNumber, String courtCode, LocalDate from, LocalDate to);
}
