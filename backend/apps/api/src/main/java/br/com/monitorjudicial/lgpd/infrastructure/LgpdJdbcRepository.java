package br.com.monitorjudicial.lgpd.infrastructure;

import br.com.monitorjudicial.lgpd.application.ClientDataExport;
import br.com.monitorjudicial.lgpd.application.ClientDataExport.ClientProfile;
import br.com.monitorjudicial.lgpd.application.ClientDataExport.NotificationRecord;
import br.com.monitorjudicial.lgpd.application.ClientDataExport.ProcessRecord;
import br.com.monitorjudicial.lgpd.application.DataRetentionPort;
import br.com.monitorjudicial.lgpd.application.LgpdExportPort;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class LgpdJdbcRepository implements LgpdExportPort, DataRetentionPort {

    private static final long RETENTION_LOCK_ID = 8_741_902_311L;

    private final JdbcClient jdbc;

    @Override
    public Optional<ClientDataExport> loadClientExport(UUID clientId, Instant exportedAt) {
        Optional<ClientProfile> profile = jdbc.sql("""
                select id, name, document, email, phone, status, notify_email, notify_whatsapp,
                       legal_basis, privacy_notice_version, consent_recorded_at, anonymized_at,
                       created_at, updated_at
                  from clients
                 where id = :id
                """)
                .param("id", clientId)
                .query(LgpdJdbcRepository::mapClient)
                .optional();
        if (profile.isEmpty()) {
            return Optional.empty();
        }
        List<ProcessRecord> processes = jdbc.sql("""
                select p.id, p.process_number, p.court_code, p.subject, p.status, p.confidential,
                       max(pub.publication_date) as last_pub_date, p.created_at
                  from processes p
                  left join publications pub on pub.process_id = p.id
                 where p.client_id = :clientId
                 group by p.id
                 order by p.created_at desc
                """)
                .param("clientId", clientId)
                .query(LgpdJdbcRepository::mapProcess)
                .list();
        List<NotificationRecord> notifications = jdbc.sql("""
                select id, process_id, channel, status, sent_at, created_at
                  from notifications
                 where client_id = :clientId
                 order by created_at desc
                """)
                .param("clientId", clientId)
                .query(LgpdJdbcRepository::mapNotification)
                .list();
        return Optional.of(new ClientDataExport(exportedAt, profile.get(), processes, notifications));
    }

    @Override
    public boolean tryAcquireRetentionLock() {
        Boolean acquired = jdbc.sql("select pg_try_advisory_lock(:id)")
                .param("id", RETENTION_LOCK_ID)
                .query(Boolean.class)
                .single();
        return Boolean.TRUE.equals(acquired);
    }

    @Override
    public void releaseRetentionLock() {
        jdbc.sql("select pg_advisory_unlock(:id)")
                .param("id", RETENTION_LOCK_ID)
                .query(Boolean.class)
                .single();
    }

    @Override
    public int purgeRefreshTokensBefore(Instant cutoff) {
        return jdbc.sql("""
                delete from refresh_tokens
                 where (revoked_at is not null and revoked_at < :cutoff)
                    or expires_at < :cutoff
                """)
                .param("cutoff", Timestamp.from(cutoff))
                .update();
    }

    @Override
    public int purgeMonitoringJobsBefore(Instant cutoff) {
        return jdbc.sql("delete from monitoring_jobs where created_at < :cutoff")
                .param("cutoff", Timestamp.from(cutoff))
                .update();
    }

    @Override
    public int purgeNotificationsBefore(Instant cutoff) {
        return jdbc.sql("delete from notifications where created_at < :cutoff")
                .param("cutoff", Timestamp.from(cutoff))
                .update();
    }

    @Override
    public int purgeAuditLogsBefore(Instant cutoff) {
        return jdbc.sql("delete from audit_logs where occurred_at < :cutoff")
                .param("cutoff", Timestamp.from(cutoff))
                .update();
    }

    private static ClientProfile mapClient(ResultSet rs, int rowNum) throws SQLException {
        return new ClientProfile(
                rs.getObject("id", UUID.class),
                rs.getString("name"),
                rs.getString("document"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("status"),
                rs.getBoolean("notify_email"),
                rs.getBoolean("notify_whatsapp"),
                rs.getString("legal_basis"),
                rs.getString("privacy_notice_version"),
                toInstant(rs.getTimestamp("consent_recorded_at")),
                toInstant(rs.getTimestamp("anonymized_at")),
                toInstant(rs.getTimestamp("created_at")),
                toInstant(rs.getTimestamp("updated_at")));
    }

    private static ProcessRecord mapProcess(ResultSet rs, int rowNum) throws SQLException {
        var pubDate = rs.getDate("last_pub_date");
        LocalDate lastPub = pubDate == null ? null : pubDate.toLocalDate();
        return new ProcessRecord(
                rs.getObject("id", UUID.class),
                rs.getString("process_number"),
                rs.getString("court_code"),
                rs.getString("subject"),
                rs.getString("status"),
                rs.getBoolean("confidential"),
                lastPub,
                toInstant(rs.getTimestamp("created_at")));
    }

    private static NotificationRecord mapNotification(ResultSet rs, int rowNum) throws SQLException {
        return new NotificationRecord(
                rs.getObject("id", UUID.class),
                rs.getObject("process_id", UUID.class),
                rs.getString("channel"),
                rs.getString("status"),
                toInstant(rs.getTimestamp("sent_at")),
                toInstant(rs.getTimestamp("created_at")));
    }

    private static Instant toInstant(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }
}
