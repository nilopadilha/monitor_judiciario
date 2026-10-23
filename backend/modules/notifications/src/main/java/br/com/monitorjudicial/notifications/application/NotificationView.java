package br.com.monitorjudicial.notifications.application;

import br.com.monitorjudicial.notifications.domain.Channel;
import br.com.monitorjudicial.notifications.domain.Notification;
import br.com.monitorjudicial.notifications.domain.NotificationStatus;
import java.time.Instant;
import java.util.UUID;

public record NotificationView(
        UUID id,
        UUID processId,
        String processNumber,
        UUID clientId,
        String clientName,
        Channel channel,
        String recipient,
        String subject,
        NotificationStatus status,
        int attempts,
        String errorMessage,
        Instant createdAt,
        Instant sentAt) {

    static NotificationView of(Notification n, String processNumber, String clientName) {
        return new NotificationView(n.id(), n.processId(), processNumber, n.clientId(), clientName, n.channel(),
                n.recipient(), n.subject(), n.status(), n.attempts(), n.errorMessage().orElse(null), n.createdAt(),
                n.sentAt().orElse(null));
    }
}
