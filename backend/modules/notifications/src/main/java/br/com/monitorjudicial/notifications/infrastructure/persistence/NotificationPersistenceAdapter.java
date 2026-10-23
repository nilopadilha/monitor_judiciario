package br.com.monitorjudicial.notifications.infrastructure.persistence;

import br.com.monitorjudicial.notifications.domain.Channel;
import br.com.monitorjudicial.notifications.domain.Notification;
import br.com.monitorjudicial.notifications.domain.port.NotificationRepository;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class NotificationPersistenceAdapter implements NotificationRepository {

    private final NotificationJpaRepository jpa;

    @Override
    public Notification save(Notification n) {
        NotificationEntity e = new NotificationEntity();
        e.setId(n.id());
        e.setEventId(n.eventId().orElse(null));
        e.setProcessId(n.processId());
        e.setClientId(n.clientId());
        e.setChannel(n.channel());
        e.setRecipient(n.recipient());
        e.setSubject(n.subject());
        e.setMessage(n.message());
        e.setStatus(n.status());
        e.setAttempts(n.attempts());
        e.setErrorMessage(n.errorMessage().orElse(null));
        e.setCreatedAt(n.createdAt());
        e.setSentAt(n.sentAt().orElse(null));
        jpa.save(e);
        return n;
    }

    @Override
    public boolean existsByEventAndChannel(UUID eventId, Channel channel) {
        return jpa.existsByEventIdAndChannel(eventId, channel);
    }

    @Override
    public PageResult<Notification> search(NotificationFilter filter, PageQuery page) {
        Specification<NotificationEntity> spec = (root, query, cb) -> cb.conjunction();
        if (filter.clientId() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("clientId"), filter.clientId()));
        }
        if (filter.processId() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("processId"), filter.processId()));
        }
        if (filter.channel() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("channel"), filter.channel()));
        }
        if (filter.status() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), filter.status()));
        }
        var result = jpa.findAll(spec, PageRequest.of(page.page(), page.size(), Sort.by(Sort.Order.desc("createdAt"))));
        return PageResult.of(result.map(NotificationPersistenceAdapter::toDomain).getContent(), page.page(),
                page.size(), result.getTotalElements());
    }

    private static Notification toDomain(NotificationEntity e) {
        return new Notification(e.getId(), e.getEventId(), e.getProcessId(), e.getClientId(), e.getChannel(),
                e.getRecipient(), e.getSubject(), e.getMessage(), e.getStatus(), e.getAttempts(), e.getErrorMessage(),
                e.getCreatedAt(), e.getSentAt());
    }
}
