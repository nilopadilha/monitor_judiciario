package br.com.monitorjudicial.dashboard.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Indicadores do painel (fase 10). */
public record DashboardSummary(
        long monitoredProcesses,
        long pausedProcesses,
        long processesWithNews,
        long processesWithFailedCheck,
        long unreadPublications,
        long notificationsToday,
        long failedNotificationsToday,
        LastExecution lastExecution,
        List<DailyCount> publicationsPerDay,
        List<TribunalCount> publicationsByTribunal) {

    public record LastExecution(UUID id, String status, Instant startedAt, Instant finishedAt, int newPublications,
            int failedJobs) {
    }

    public record DailyCount(LocalDate date, long count) {
    }

    public record TribunalCount(String courtCode, long count) {
    }
}
