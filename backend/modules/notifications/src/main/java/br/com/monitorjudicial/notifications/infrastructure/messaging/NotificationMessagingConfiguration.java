package br.com.monitorjudicial.notifications.infrastructure.messaging;

import br.com.monitorjudicial.monitoring.api.MonitoringEvents;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Fila de notificações com dead-letter para mensagens que falham após as retentativas (ADR-0004). */
@Configuration
class NotificationMessagingConfiguration {

    static final String QUEUE = "notifications.process-evolution";
    static final String DEAD_LETTER_EXCHANGE = "monitor.events.dlx";
    static final String DEAD_LETTER_QUEUE = QUEUE + ".dlq";

    @Bean
    Queue notificationsQueue() {
        return QueueBuilder.durable(QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(DEAD_LETTER_QUEUE)
                .build();
    }

    @Bean
    Binding notificationsBinding(Queue notificationsQueue) {
        return BindingBuilder.bind(notificationsQueue)
                .to(new TopicExchange(MonitoringEvents.EXCHANGE, true, false))
                .with(MonitoringEvents.PROCESS_EVOLUTION_DETECTED);
    }

    @Bean
    DirectExchange notificationsDeadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue notificationsDeadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Binding notificationsDeadLetterBinding(Queue notificationsDeadLetterQueue,
            DirectExchange notificationsDeadLetterExchange) {
        return BindingBuilder.bind(notificationsDeadLetterQueue).to(notificationsDeadLetterExchange)
                .with(DEAD_LETTER_QUEUE);
    }

    @Bean
    MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
