package br.com.monitorjudicial.monitoring.infrastructure.scheduling;

import br.com.monitorjudicial.monitoring.application.RunMonitoringService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Disparo diário (fase 3). Cron e fuso configuráveis; uma única réplica executa por vez (advisory lock). */
@Component
@ConditionalOnProperty(name = "monitoring.scheduler.enabled", havingValue = "true", matchIfMissing = true)
class MonitoringScheduler {

    private final RunMonitoringService runMonitoring;

    MonitoringScheduler(RunMonitoringService runMonitoring) {
        this.runMonitoring = runMonitoring;
    }

    @Scheduled(cron = "${monitoring.cron:0 0 6 * * *}", zone = "${monitoring.zone:America/Sao_Paulo}")
    void daily() {
        runMonitoring.startScheduled();
    }
}
