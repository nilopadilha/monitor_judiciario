package br.com.monitorjudicial.lgpd.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Art. 15–16 LGPD: expurgo automatizado de registros além da janela de retenção configurada. */
@Service
public class DataRetentionService {

    private static final Logger log = LoggerFactory.getLogger(DataRetentionService.class);

    public record RetentionReport(int refreshTokens, int monitoringJobs, int notifications, int auditLogs) {
    }

    private final DataRetentionPort port;
    private final Clock clock;
    private final int refreshTokensDays;
    private final int monitoringJobsDays;
    private final int notificationsDays;
    private final int auditLogsDays;

    public DataRetentionService(DataRetentionPort port, Clock clock,
            @Value("${retention.refresh-tokens-days:30}") int refreshTokensDays,
            @Value("${retention.monitoring-jobs-days:90}") int monitoringJobsDays,
            @Value("${retention.notifications-days:365}") int notificationsDays,
            @Value("${retention.audit-logs-days:1825}") int auditLogsDays) {
        this.port = port;
        this.clock = clock;
        this.refreshTokensDays = refreshTokensDays;
        this.monitoringJobsDays = monitoringJobsDays;
        this.notificationsDays = notificationsDays;
        this.auditLogsDays = auditLogsDays;
    }

    @Transactional
    public RetentionReport purgeExpired() {
        if (!port.tryAcquireRetentionLock()) {
            return new RetentionReport(0, 0, 0, 0);
        }
        try {
            Instant now = clock.instant();
            int tokens = port.purgeRefreshTokensBefore(now.minus(Duration.ofDays(refreshTokensDays)));
            int jobs = port.purgeMonitoringJobsBefore(now.minus(Duration.ofDays(monitoringJobsDays)));
            int notifs = port.purgeNotificationsBefore(now.minus(Duration.ofDays(notificationsDays)));
            int audits = port.purgeAuditLogsBefore(now.minus(Duration.ofDays(auditLogsDays)));
            if (tokens + jobs + notifs + audits > 0) {
                log.info("Retenção LGPD executada: tokens={}, jobs={}, notifications={}, auditLogs={}",
                        tokens, jobs, notifs, audits);
            }
            return new RetentionReport(tokens, jobs, notifs, audits);
        } finally {
            port.releaseRetentionLock();
        }
    }
}
