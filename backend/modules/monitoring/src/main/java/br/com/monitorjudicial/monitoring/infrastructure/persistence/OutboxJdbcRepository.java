package br.com.monitorjudicial.monitoring.infrastructure.persistence;

import br.com.monitorjudicial.monitoring.domain.OutboxEvent;
import br.com.monitorjudicial.monitoring.domain.port.OutboxRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Outbox via JDBC: participa da transação JPA corrente (mesmo DataSource) e suporta SKIP LOCKED. */
@Repository
public class OutboxJdbcRepository implements OutboxRepository {

    private final JdbcClient jdbc;

    public OutboxJdbcRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void append(OutboxEvent event) {
        jdbc.sql("""
                insert into outbox_events (id, aggregate_type, aggregate_id, event_type, payload, correlation_id,
                                           created_at, attempts)
                values (:id, :aggregateType, :aggregateId, :eventType, cast(:payload as jsonb), :correlationId,
                        :createdAt, 0)
                """)
                .param("id", event.id())
                .param("aggregateType", event.aggregateType())
                .param("aggregateId", event.aggregateId())
                .param("eventType", event.eventType())
                .param("payload", event.payload())
                .param("correlationId", event.correlationId())
                .param("createdAt", Timestamp.from(event.createdAt()))
                .update();
    }

    /** Bloqueia um lote de eventos pendentes; outras réplicas pulam as linhas bloqueadas. */
    public List<OutboxEvent> lockPending(int limit) {
        return jdbc.sql("""
                select id, aggregate_type, aggregate_id, event_type, payload::text as payload, correlation_id,
                       created_at
                from outbox_events
                where published_at is null and attempts < :maxAttempts
                order by created_at
                limit :limit
                for update skip locked
                """)
                .param("limit", limit)
                .param("maxAttempts", 20)
                .query((rs, n) -> new OutboxEvent(rs.getObject("id", UUID.class), rs.getString("aggregate_type"),
                        rs.getObject("aggregate_id", UUID.class), rs.getString("event_type"), rs.getString("payload"),
                        rs.getString("correlation_id"), rs.getTimestamp("created_at").toInstant()))
                .list();
    }

    public void markPublished(UUID id, Instant at) {
        jdbc.sql("update outbox_events set published_at = :at where id = :id")
                .param("at", Timestamp.from(at)).param("id", id).update();
    }

    public void markFailed(UUID id, String error) {
        jdbc.sql("update outbox_events set attempts = attempts + 1, last_error = :error where id = :id")
                .param("error", error == null ? null : error.substring(0, Math.min(error.length(), 500)))
                .param("id", id).update();
    }

    public long countPending() {
        return jdbc.sql("select count(*) from outbox_events where published_at is null").query(Long.class).single();
    }
}
