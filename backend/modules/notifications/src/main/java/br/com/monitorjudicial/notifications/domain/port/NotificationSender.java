package br.com.monitorjudicial.notifications.domain.port;

import br.com.monitorjudicial.notifications.domain.Channel;
import br.com.monitorjudicial.notifications.domain.OutgoingMessage;

public interface NotificationSender {

    Channel channel();

    /** @throws br.com.monitorjudicial.notifications.domain.NotificationDeliveryException se a entrega falhar */
    void send(OutgoingMessage message);
}
