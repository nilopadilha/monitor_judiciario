package br.com.monitorjudicial.clients.api;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Contrato público do módulo de clientes. */
public interface ClientDirectory {

    Optional<ClientSummary> findById(UUID id);

    Map<UUID, ClientSummary> findByIds(Collection<UUID> ids);
}
