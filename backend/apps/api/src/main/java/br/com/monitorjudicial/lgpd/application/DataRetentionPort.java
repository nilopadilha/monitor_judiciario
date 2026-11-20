package br.com.monitorjudicial.lgpd.application;

import java.time.Instant;

public interface DataRetentionPort {

    boolean tryAcquireRetentionLock();

    void releaseRetentionLock();

    int purgeRefreshTokensBefore(Instant cutoff);

    int purgeMonitoringJobsBefore(Instant cutoff);

    int purgeNotificationsBefore(Instant cutoff);

    int purgeAuditLogsBefore(Instant cutoff);
}
