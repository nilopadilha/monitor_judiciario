package br.com.monitorjudicial.notifications.domain.port;

import br.com.monitorjudicial.notifications.domain.Channel;
import br.com.monitorjudicial.notifications.domain.Notification;
import br.com.monitorjudicial.notifications.domain.NotificationStatus;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import java.util.UUID;

public interface NotificationRepository {

    Notification save(Notification notification);

    boolean existsByEventAndChannel(UUID eventId, Channel channel);

    PageResult<Notification> search(NotificationFilter filter, PageQuery page);

    record NotificationFilter(UUID clientId, UUID processId, Channel channel, NotificationStatus status) {
    }
}
