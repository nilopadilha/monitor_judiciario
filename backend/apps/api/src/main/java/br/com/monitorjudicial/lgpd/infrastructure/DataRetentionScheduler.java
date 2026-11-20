package br.com.monitorjudicial.lgpd.infrastructure;

import br.com.monitorjudicial.lgpd.application.DataRetentionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "retention.enabled", havingValue = "true", matchIfMissing = true)
class DataRetentionScheduler {

    private final DataRetentionService retention;

    DataRetentionScheduler(DataRetentionService retention) {
        this.retention = retention;
    }

    @Scheduled(cron = "${retention.cron:0 30 3 * * *}", zone = "${monitoring.zone:America/Sao_Paulo}")
    void runDailyPurge() {
        retention.purgeExpired();
    }
}
