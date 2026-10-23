package br.com.monitorjudicial.processes.api;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Porta implementada pelo módulo monitoring (inversão de dependência): processes exibe e filtra pela situação do
 * monitoramento sem depender de monitoring.
 */
public interface ProcessMonitoringInsights {

    Map<UUID, ProcessMonitoringStatus> statusOf(Collection<UUID> processIds);

    Set<UUID> processesWithUnreadPublications();

    Set<UUID> processesWithFailedLastCheck();
}
