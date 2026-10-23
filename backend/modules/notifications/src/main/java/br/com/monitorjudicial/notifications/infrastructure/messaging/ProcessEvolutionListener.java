package br.com.monitorjudicial.notifications.infrastructure.messaging;

import br.com.monitorjudicial.monitoring.api.ProcessEvolutionDetected;
import br.com.monitorjudicial.notifications.application.NotificationService;
import br.com.monitorjudicial.shared.observability.CorrelationId;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
class ProcessEvolutionListener {

    private final NotificationService notifications;

    ProcessEvolutionListener(NotificationService notifications) {
        this.notifications = notifications;
    }

    @RabbitListener(queues = NotificationMessagingConfiguration.QUEUE)
    void onEvolution(ProcessEvolutionDetected event,
            @Header(name = CorrelationId.HEADER, required = false) String correlationId) {
        MDC.put(CorrelationId.MDC_KEY, correlationId != null ? correlationId : event.eventId().toString());
        try {
            notifications.handle(event);
        } finally {
            MDC.remove(CorrelationId.MDC_KEY);
        }
    }
}
