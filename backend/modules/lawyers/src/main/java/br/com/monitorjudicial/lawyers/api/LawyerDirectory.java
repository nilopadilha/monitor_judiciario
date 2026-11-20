package br.com.monitorjudicial.lawyers.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Contrato público do módulo de advogados (ADR-0001). */
public interface LawyerDirectory {

    Optional<LawyerSummary> findById(UUID id);

    Map<UUID, LawyerSummary> findByIds(Collection<UUID> ids);

    List<LawyerSummary> findActive();
}
