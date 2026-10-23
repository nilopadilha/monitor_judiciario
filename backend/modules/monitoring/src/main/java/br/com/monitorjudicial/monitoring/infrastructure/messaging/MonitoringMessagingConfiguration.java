package br.com.monitorjudicial.monitoring.infrastructure.messaging;

import br.com.monitorjudicial.monitoring.api.MonitoringEvents;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class MonitoringMessagingConfiguration {

    @Bean
    TopicExchange monitorEventsExchange() {
        return new TopicExchange(MonitoringEvents.EXCHANGE, true, false);
    }
}
