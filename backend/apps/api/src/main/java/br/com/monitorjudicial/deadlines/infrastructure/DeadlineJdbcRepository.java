package br.com.monitorjudicial.deadlines.infrastructure;

import br.com.monitorjudicial.deadlines.application.DeadlinePort;
import br.com.monitorjudicial.deadlines.application.DeadlineSummary;
import br.com.monitorjudicial.deadlines.application.DeadlineView;
import br.com.monitorjudicial.deadlines.domain.DeadlineCalculator;
import br.com.monitorjudicial.deadlines.domain.DeadlineStatus;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class DeadlineJdbcRepository implements DeadlinePort {

    private static final long ALERT_LOCK_ID = 9_124_803_711L;

    private static final String BASE_SELECT = """
            select d.id, d.process_id, p.process_number, p.court_code, c.name as client_name,
                   d.publication_id, d.lawyer_id, l.name as lawyer_name, d.title, d.description,
                   d.start_date, d.due_date, d.days_count, d.business_days, d.status,
                   d.auto_detected, d.completed_at, d.created_at
              from deadlines d
              join processes p on p.id = d.process_id
              join clients c on c.id = p.client_id
              left join lawyers l on l.id = d.lawyer_id
            """;

    private final JdbcClient jdbc;

    @Override
    public boolean existsForPublication(UUID publicationId) {
        Integer count = jdbc.sql("select count(*) from deadlines where publication_id = :pubId")
                .param("pubId", publicationId)
                .query(Integer.class)
                .single();
        return count != null && count > 0;
    }

    @Override
    public void insert(UUID id, UUID processId, UUID publicationId, UUID lawyerId, String title, String description,
            LocalDate startDate, LocalDate dueDate, Integer daysCount, boolean businessDays, boolean autoDetected,
            Instant now) {
        jdbc.sql("""
                insert into deadlines (id, process_id, publication_id, lawyer_id, title, description,
                    start_date, due_date, days_count, business_days, status, auto_detected, created_at, updated_at)
                values (:id, :processId, :pubId, :lawyerId, :title, :description,
                    :startDate, :dueDate, :daysCount, :businessDays, 'OPEN', :autoDetected, :now, :now)
                """)
                .param("id", id)
                .param("processId", processId)
                .param("pubId", publicationId)
                .param("lawyerId", lawyerId)
                .param("title", title)
                .param("description", description)
                .param("startDate", Date.valueOf(startDate))
                .param("dueDate", Date.valueOf(dueDate))
                .param("daysCount", daysCount)
                .param("businessDays", businessDays)
                .param("autoDetected", autoDetected)
                .param("now", Timestamp.from(now))
                .update();
    }

    @Override
    public Optional<DeadlineView> findById(UUID id, LocalDate today) {
        return jdbc.sql(BASE_SELECT + " where d.id = :id")
                .param("id", id)
                .query((rs, rowNum) -> mapView(rs, today))
                .optional();
    }

    @Override
    public PageResult<DeadlineView> search(UUID lawyerId, UUID processId, DeadlineStatus status, PageQuery page,
            LocalDate today) {
        StringBuilder where = new StringBuilder(" where 1=1");
        Map<String, Object> params = new LinkedHashMap<>();
        if (lawyerId != null) {
            where.append(" and d.lawyer_id = :lawyerId");
            params.put("lawyerId", lawyerId);
        }
        if (processId != null) {
            where.append(" and d.process_id = :processId");
            params.put("processId", processId);
        }
        if (status != null) {
            where.append(" and d.status = :status");
            params.put("status", status.name());
        }
        var countSpec = jdbc.sql("select count(*) from deadlines d" + where);
        for (var e : params.entrySet()) {
            countSpec = countSpec.param(e.getKey(), e.getValue());
        }
        long total = countSpec.query(Long.class).single();

        var dataSpec = jdbc.sql(BASE_SELECT + where + " order by d.due_date asc, d.created_at desc limit :limit offset :offset");
        for (var e : params.entrySet()) {
            dataSpec = dataSpec.param(e.getKey(), e.getValue());
        }
        List<DeadlineView> content = dataSpec
                .param("limit", page.size())
                .param("offset", page.page() * page.size())
                .query((rs, rowNum) -> mapView(rs, today))
                .list();
        return PageResult.of(content, page.page(), page.size(), total);
    }

    @Override
    public DeadlineSummary summary(LocalDate today) {
        return jdbc.sql("""
                select
                  count(*) filter (where status = 'OPEN' and due_date < :today) as overdue,
                  count(*) filter (where status = 'OPEN' and due_date = :today) as due_today,
                  count(*) filter (where status = 'OPEN' and due_date > :today and due_date <= :in3) as due_3,
                  count(*) filter (where status = 'OPEN' and due_date > :today and due_date <= :in7) as due_7,
                  count(*) filter (where status = 'OPEN') as open_total
                from deadlines
                """)
                .param("today", Date.valueOf(today))
                .param("in3", Date.valueOf(today.plusDays(3)))
                .param("in7", Date.valueOf(today.plusDays(7)))
                .query((rs, rowNum) -> new DeadlineSummary(
                        rs.getLong("overdue"),
                        rs.getLong("due_today"),
                        rs.getLong("due_3"),
                        rs.getLong("due_7"),
                        rs.getLong("open_total")))
                .single();
    }

    @Override
    public boolean updateStatus(UUID id, DeadlineStatus status, Instant completedAt, Instant now) {
        int updated = jdbc.sql("""
                update deadlines
                   set status = :status,
                       completed_at = :completedAt,
                       updated_at = :now
                 where id = :id
                """)
                .param("status", status.name())
                .param("completedAt", completedAt == null ? null : Timestamp.from(completedAt))
                .param("now", Timestamp.from(now))
                .param("id", id)
                .update();
        return updated > 0;
    }

    @Override
    public boolean tryAcquireAlertLock() {
        Boolean acquired = jdbc.sql("select pg_try_advisory_lock(:id)")
                .param("id", ALERT_LOCK_ID)
                .query(Boolean.class)
                .single();
        return Boolean.TRUE.equals(acquired);
    }

    @Override
    public void releaseAlertLock() {
        jdbc.sql("select pg_advisory_unlock(:id)")
                .param("id", ALERT_LOCK_ID)
                .query(Boolean.class)
                .single();
    }

    @Override
    public List<DueDeadlineAlert> findDueForAlert(LocalDate maxDueDate, LocalDate today) {
        return jdbc.sql("""
                select d.id, p.process_number, c.name as client_name, d.title, d.due_date,
                       l.name as lawyer_name, l.email as lawyer_email, l.notify_email
                  from deadlines d
                  join processes p on p.id = d.process_id
                  join clients c on c.id = p.client_id
                  join lawyers l on l.id = d.lawyer_id
                 where d.status = 'OPEN'
                   and l.active = true
                   and d.due_date <= :maxDue
                   and (d.last_alert_date is null or d.last_alert_date < :today)
                 order by d.due_date asc
                """)
                .param("maxDue", Date.valueOf(maxDueDate))
                .param("today", Date.valueOf(today))
                .query((rs, rowNum) -> new DueDeadlineAlert(
                        rs.getObject("id", UUID.class),
                        formatCnj(rs.getString("process_number")),
                        rs.getString("client_name"),
                        rs.getString("title"),
                        rs.getDate("due_date").toLocalDate(),
                        rs.getString("lawyer_name"),
                        rs.getString("lawyer_email"),
                        rs.getBoolean("notify_email")))
                .list();
    }

    @Override
    public void markAlerted(UUID deadlineId, LocalDate today, Instant now) {
        jdbc.sql("update deadlines set last_alert_date = :today, updated_at = :now where id = :id")
                .param("today", Date.valueOf(today))
                .param("now", Timestamp.from(now))
                .param("id", deadlineId)
                .update();
    }

    private static DeadlineView mapView(ResultSet rs, LocalDate today) throws SQLException {
        DeadlineStatus status = DeadlineStatus.valueOf(rs.getString("status"));
        LocalDate dueDate = rs.getDate("due_date").toLocalDate();
        Integer daysCount = rs.getObject("days_count") == null ? null : rs.getInt("days_count");
        Timestamp completedTs = rs.getTimestamp("completed_at");
        return new DeadlineView(
                rs.getObject("id", UUID.class),
                rs.getObject("process_id", UUID.class),
                formatCnj(rs.getString("process_number")),
                rs.getString("court_code"),
                rs.getString("client_name"),
                rs.getObject("publication_id", UUID.class),
                rs.getObject("lawyer_id", UUID.class),
                rs.getString("lawyer_name"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getDate("start_date").toLocalDate(),
                dueDate,
                daysCount,
                rs.getBoolean("business_days"),
                status,
                DeadlineCalculator.urgencyOf(status, dueDate, today),
                ChronoUnit.DAYS.between(today, dueDate),
                rs.getBoolean("auto_detected"),
                completedTs == null ? null : completedTs.toInstant(),
                rs.getTimestamp("created_at").toInstant());
    }

    private static String formatCnj(String d) {
        if (d == null || d.length() != 20) {
            return d;
        }
        return d.substring(0, 7) + "-" + d.substring(7, 9) + "." + d.substring(9, 13) + "."
                + d.charAt(13) + "." + d.substring(14, 16) + "." + d.substring(16);
    }
}
