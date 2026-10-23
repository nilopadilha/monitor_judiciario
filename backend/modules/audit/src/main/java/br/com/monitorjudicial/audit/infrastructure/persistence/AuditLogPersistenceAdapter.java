package br.com.monitorjudicial.audit.infrastructure.persistence;

import br.com.monitorjudicial.audit.domain.AuditLog;
import br.com.monitorjudicial.audit.domain.port.AuditLogRepository;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class AuditLogPersistenceAdapter implements AuditLogRepository {

    private final AuditLogJpaRepository jpa;

    @Override
    public void save(AuditLog log) {
        AuditLogEntity e = new AuditLogEntity();
        e.setId(log.id());
        e.setActorId(log.actorId());
        e.setActorEmail(log.actorEmail());
        e.setAction(log.action());
        e.setResourceType(log.resourceType());
        e.setResourceId(log.resourceId());
        e.setDetails(log.details());
        e.setCorrelationId(log.correlationId());
        e.setOccurredAt(log.occurredAt());
        jpa.save(e);
    }

    @Override
    public PageResult<AuditLog> search(AuditFilter f, PageQuery page) {
        Specification<AuditLogEntity> spec = (root, query, cb) -> cb.conjunction();
        if (f.resourceType() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("resourceType"), f.resourceType()));
        }
        if (f.resourceId() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("resourceId"), f.resourceId()));
        }
        if (f.actorId() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("actorId"), f.actorId()));
        }
        if (f.from() != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("occurredAt"), f.from()));
        }
        if (f.to() != null) {
            spec = spec.and((root, query, cb) -> cb.lessThan(root.get("occurredAt"), f.to()));
        }
        var result = jpa.findAll(spec, PageRequest.of(page.page(), page.size(), Sort.by(Sort.Order.desc("occurredAt"))));
        return PageResult.of(result.map(e -> new AuditLog(e.getId(), e.getActorId(), e.getActorEmail(), e.getAction(),
                e.getResourceType(), e.getResourceId(), e.getDetails(), e.getCorrelationId(), e.getOccurredAt()))
                .getContent(), page.page(), page.size(), result.getTotalElements());
    }
}
