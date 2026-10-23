package br.com.monitorjudicial.monitoring.application;

import br.com.monitorjudicial.processes.api.MonitoredProcess;
import java.time.LocalDate;
import java.time.ZoneId;

/** Janela de datas de disponibilização a consultar para um processo. */
record CheckWindow(LocalDate from, LocalDate to) {

    static CheckWindow of(MonitoredProcess process, LocalDate today, ZoneId zone, int lookbackDays, int overlapDays) {
        LocalDate from = process.lastCheckedAt() != null
                ? process.lastCheckedAt().atZone(zone).toLocalDate().minusDays(overlapDays)
                : process.createdAt().atZone(zone).toLocalDate().minusDays(lookbackDays);
        return new CheckWindow(from.isAfter(today) ? today : from, today);
    }
}
