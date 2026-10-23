package br.com.monitorjudicial.dashboard.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Porta do lado de leitura (CQRS): consultas agregadas, sem hidratar agregados. {@code clientId} nulo = todos. */
public interface DashboardReadModel {

    long countProcesses(UUID clientId, String status);

    long countProcessesWithUnread(UUID clientId);

    long countProcessesWithFailedLastCheck(UUID clientId);

    long countUnreadPublications(UUID clientId);

    long countNotificationsSince(UUID clientId, Instant since, String status);

    Optional<DashboardSummary.LastExecution> lastExecution();

    List<DashboardSummary.DailyCount> publicationsPerDay(UUID clientId, LocalDate from, LocalDate to);

    List<DashboardSummary.TribunalCount> publicationsByTribunal(UUID clientId, LocalDate from, LocalDate to);
}
