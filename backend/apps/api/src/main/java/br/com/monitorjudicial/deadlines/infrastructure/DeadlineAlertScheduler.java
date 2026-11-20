package br.com.monitorjudicial.deadlines.infrastructure;

import br.com.monitorjudicial.deadlines.application.DeadlineService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "deadlines.alerts.enabled", havingValue = "true", matchIfMissing = true)
class DeadlineAlertScheduler {

    private final DeadlineService deadlines;

    DeadlineAlertScheduler(DeadlineService deadlines) {
        this.deadlines = deadlines;
    }

    @Scheduled(cron = "${deadlines.alert-cron:0 30 7 * * *}", zone = "${monitoring.zone:America/Sao_Paulo}")
    void notifyDueDeadlines() {
        deadlines.sendDueAlerts();
    }
}
