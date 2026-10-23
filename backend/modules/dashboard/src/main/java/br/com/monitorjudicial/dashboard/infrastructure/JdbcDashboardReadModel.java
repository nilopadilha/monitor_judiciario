package br.com.monitorjudicial.dashboard.infrastructure;

import br.com.monitorjudicial.dashboard.application.DashboardReadModel;
import br.com.monitorjudicial.dashboard.application.DashboardSummary;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Consultas de leitura do painel. Lê tabelas de vários módulos por SQL — acoplamento aceito e restrito ao lado de
 * leitura; nenhuma escrita passa por aqui.
 */
@Repository
class JdbcDashboardReadModel implements DashboardReadModel {

    private final JdbcClient jdbc;

    JdbcDashboardReadModel(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long countProcesses(UUID clientId, String status) {
        return jdbc.sql("select count(*) from processes where status = :status "
                        + "and (cast(:clientId as uuid) is null or client_id = :clientId)")
                .param("status", status).param("clientId", clientId).query(Long.class).single();
    }

    @Override
    public long countProcessesWithUnread(UUID clientId) {
        return jdbc.sql("""
                select count(distinct p.process_id) from publications p
                join processes pr on pr.id = p.process_id and pr.status = 'ACTIVE'
                where p.read_at is null and (cast(:clientId as uuid) is null or p.client_id = :clientId)
                """).param("clientId", clientId).query(Long.class).single();
    }

    @Override
    public long countProcessesWithFailedLastCheck(UUID clientId) {
        return jdbc.sql("""
                select count(*) from (
                    select distinct on (j.process_id) j.process_id, j.status from monitoring_jobs j
                    order by j.process_id, j.created_at desc) last
                join processes pr on pr.id = last.process_id and pr.status = 'ACTIVE'
                where last.status = 'FAILED' and (cast(:clientId as uuid) is null or pr.client_id = :clientId)
                """).param("clientId", clientId).query(Long.class).single();
    }

    @Override
    public long countUnreadPublications(UUID clientId) {
        return jdbc.sql("select count(*) from publications where read_at is null "
                        + "and (cast(:clientId as uuid) is null or client_id = :clientId)")
                .param("clientId", clientId).query(Long.class).single();
    }

    @Override
    public long countNotificationsSince(UUID clientId, Instant since, String status) {
        return jdbc.sql("select count(*) from notifications where created_at >= :since "
                        + "and (cast(:status as varchar) is null or status = :status) "
                        + "and (cast(:clientId as uuid) is null or client_id = :clientId)")
                .param("since", Timestamp.from(since)).param("status", status).param("clientId", clientId)
                .query(Long.class).single();
    }

    @Override
    public Optional<DashboardSummary.LastExecution> lastExecution() {
        return jdbc.sql("""
                select id, status, started_at, finished_at, new_publications, failed_jobs
                from monitoring_executions order by started_at desc limit 1
                """)
                .query((rs, n) -> new DashboardSummary.LastExecution(rs.getObject("id", UUID.class),
                        rs.getString("status"), rs.getTimestamp("started_at").toInstant(),
                        Optional.ofNullable(rs.getTimestamp("finished_at")).map(Timestamp::toInstant).orElse(null),
                        rs.getInt("new_publications"), rs.getInt("failed_jobs")))
                .optional();
    }

    @Override
    public List<DashboardSummary.DailyCount> publicationsPerDay(UUID clientId, LocalDate from, LocalDate to) {
        return jdbc.sql("""
                select publication_date, count(*) as total from publications
                where publication_date between :from and :to
                  and (cast(:clientId as uuid) is null or client_id = :clientId)
                group by publication_date order by publication_date
                """)
                .param("from", from).param("to", to).param("clientId", clientId)
                .query((rs, n) -> new DashboardSummary.DailyCount(rs.getObject("publication_date", LocalDate.class),
                        rs.getLong("total")))
                .list();
    }

    @Override
    public List<DashboardSummary.TribunalCount> publicationsByTribunal(UUID clientId, LocalDate from, LocalDate to) {
        return jdbc.sql("""
                select pr.court_code, count(*) as total from publications p
                join processes pr on pr.id = p.process_id
                where p.publication_date between :from and :to
                  and (cast(:clientId as uuid) is null or p.client_id = :clientId)
                group by pr.court_code order by total desc, pr.court_code
                """)
                .param("from", from).param("to", to).param("clientId", clientId)
                .query((rs, n) -> new DashboardSummary.TribunalCount(rs.getString("court_code"), rs.getLong("total")))
                .list();
    }
}
