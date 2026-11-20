package br.com.monitorjudicial.notifications.infrastructure.persistence;

import br.com.monitorjudicial.notifications.domain.Channel;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface NotificationJpaRepository extends JpaRepository<NotificationEntity, UUID>,
        JpaSpecificationExecutor<NotificationEntity> {

    boolean existsByEventIdAndChannel(UUID eventId, Channel channel);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update NotificationEntity n set n.recipient = '[anonimizado]', n.message = '[dados anonimizados - LGPD]' where n.clientId = :clientId")
    int anonymizeByClientId(@Param("clientId") UUID clientId);
}
