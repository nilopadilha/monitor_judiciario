package br.com.monitorjudicial.monitoring.infrastructure.messaging;

import br.com.monitorjudicial.monitoring.api.MonitoringEvents;
import br.com.monitorjudicial.monitoring.domain.OutboxEvent;
import br.com.monitorjudicial.monitoring.infrastructure.persistence.OutboxJdbcRepository;
import br.com.monitorjudicial.shared.observability.CorrelationId;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Publica os eventos pendentes do outbox no RabbitMQ com publisher confirms (ADR-0004). Entrega at-least-once: o
 * consumidor é idempotente.
 */
@Component
@ConditionalOnProperty(name = "outbox.relay.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final int BATCH = 100;
    private static final long CONFIRM_TIMEOUT_MS = 5_000;

    private final OutboxJdbcRepository outbox;
    private final RabbitTemplate rabbit;
    private final Clock clock;

    public OutboxRelay(OutboxJdbcRepository outbox, RabbitTemplate rabbit, Clock clock, MeterRegistry meters) {
        this.outbox = outbox;
        this.rabbit = rabbit;
        this.clock = clock;
        Gauge.builder("outbox.pending", outbox, OutboxJdbcRepository::countPending).register(meters);
    }

    @Scheduled(fixedDelayString = "${outbox.relay-interval:PT2S}")
    @Transactional
    public void relay() {
        List<OutboxEvent> pending = outbox.lockPending(BATCH);
        for (OutboxEvent event : pending) {
            try {
                rabbit.invoke(ops -> {
                    ops.send(MonitoringEvents.EXCHANGE, event.eventType(), toMessage(event));
                    ops.waitForConfirmsOrDie(CONFIRM_TIMEOUT_MS);
                    return null;
                });
                outbox.markPublished(event.id(), clock.instant());
            } catch (RuntimeException e) {
                log.warn("Falha ao publicar evento {} do outbox: {}", event.id(), e.getMessage());
                outbox.markFailed(event.id(), e.getMessage());
                return;
            }
        }
    }

    static Message toMessage(OutboxEvent event) {
        MessageBuilder builder = (MessageBuilder) MessageBuilder.withBody(event.payload().getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setContentEncoding(StandardCharsets.UTF_8.name())
                .setMessageId(event.id().toString())
                .setType(event.eventType())
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        if (event.correlationId() != null) {
            builder.setHeader(CorrelationId.HEADER, event.correlationId());
        }
        return builder.build();
    }
}
