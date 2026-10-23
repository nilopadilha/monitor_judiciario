package br.com.monitorjudicial.monitoring.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.monitorjudicial.monitoring.domain.OutboxEvent;
import br.com.monitorjudicial.monitoring.infrastructure.messaging.OutboxRelay;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;

class OutboxMessageTest {

    @Test
    void buildsPersistentJsonMessageWithIdsAndCorrelation() throws Exception {
        UUID id = UUID.randomUUID();
        OutboxEvent event = new OutboxEvent(id, "process", UUID.randomUUID(), "process.evolution-detected",
                "{\"a\":1}", "corr-123", Instant.now());
        Method toMessage = OutboxRelay.class.getDeclaredMethod("toMessage", OutboxEvent.class);
        toMessage.setAccessible(true);

        Message message = (Message) toMessage.invoke(null, event);

        assertThat(new String(message.getBody(), StandardCharsets.UTF_8)).isEqualTo("{\"a\":1}");
        assertThat(message.getMessageProperties().getContentType()).isEqualTo("application/json");
        assertThat(message.getMessageProperties().getMessageId()).isEqualTo(id.toString());
        assertThat(message.getMessageProperties().getReceivedDeliveryMode()).isNull();
        assertThat(message.getMessageProperties().getDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
        assertThat((String) message.getMessageProperties().getHeader("X-Correlation-Id")).isEqualTo("corr-123");
    }
}
