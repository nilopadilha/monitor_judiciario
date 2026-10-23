package br.com.monitorjudicial.notifications.infrastructure.persistence;

import br.com.monitorjudicial.notifications.domain.Channel;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface NotificationJpaRepository extends JpaRepository<NotificationEntity, UUID>,
        JpaSpecificationExecutor<NotificationEntity> {

    boolean existsByEventIdAndChannel(UUID eventId, Channel channel);
}
