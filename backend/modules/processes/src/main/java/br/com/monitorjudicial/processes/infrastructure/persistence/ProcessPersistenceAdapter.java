package br.com.monitorjudicial.processes.infrastructure.persistence;

import br.com.monitorjudicial.processes.domain.Process;
import br.com.monitorjudicial.processes.domain.ProcessNumber;
import br.com.monitorjudicial.processes.domain.ProcessStatus;
import br.com.monitorjudicial.processes.domain.port.ProcessRepository;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class ProcessPersistenceAdapter implements ProcessRepository {

    private final ProcessJpaRepository jpa;

    @Override
    public Process save(Process p) {
        ProcessEntity e = new ProcessEntity();
        e.setId(p.id());
        e.setClientId(p.clientId());
        e.setProcessNumber(p.number().digits());
        e.setCourtCode(p.courtCode());
        e.setSubject(p.subject().orElse(null));
        e.setStatus(p.status());
        e.setLastCheckedAt(p.lastCheckedAt().orElse(null));
        e.setCreatedAt(p.createdAt());
        e.setUpdatedAt(p.updatedAt());
        jpa.save(e);
        return p;
    }

    @Override
    public Optional<Process> findById(UUID id) {
        return jpa.findById(id).map(ProcessPersistenceAdapter::toDomain);
    }

    @Override
    public List<Process> findAllById(Collection<UUID> ids) {
        return jpa.findAllById(ids).stream().map(ProcessPersistenceAdapter::toDomain).toList();
    }

    @Override
    public boolean existsByNumber(ProcessNumber number) {
        return jpa.existsByProcessNumber(number.digits());
    }

    @Override
    public List<Process> findByStatusOrderByLastCheckedAt(ProcessStatus status) {
        return jpa.findByStatusOrderByLastChecked(status).stream().map(ProcessPersistenceAdapter::toDomain).toList();
    }

    @Override
    public void updateLastCheckedAt(UUID id, Instant checkedAt) {
        jpa.updateLastCheckedAt(id, checkedAt);
    }

    @Override
    public PageResult<Process> search(ProcessFilter filter, PageQuery page) {
        if (filter.restrictTo() != null && filter.restrictTo().isEmpty()) {
            return PageResult.of(List.of(), page.page(), page.size(), 0);
        }
        Specification<ProcessEntity> spec = (root, query, cb) -> cb.conjunction();
        if (filter.clientId() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("clientId"), filter.clientId()));
        }
        if (filter.status() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), filter.status()));
        }
        if (filter.courtCode() != null && !filter.courtCode().isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("courtCode"), filter.courtCode().toUpperCase()));
        }
        if (filter.number() != null && !filter.number().replaceAll("\\D", "").isEmpty()) {
            String digits = filter.number().replaceAll("\\D", "");
            spec = spec.and((root, query, cb) -> cb.like(root.get("processNumber"), "%" + digits + "%"));
        }
        if (filter.restrictTo() != null) {
            spec = spec.and((root, query, cb) -> root.get("id").in(filter.restrictTo()));
        }
        var result = jpa.findAll(spec, PageRequest.of(page.page(), page.size(),
                Sort.by(Sort.Order.desc("createdAt"))));
        return PageResult.of(result.map(ProcessPersistenceAdapter::toDomain).getContent(), page.page(), page.size(),
                result.getTotalElements());
    }

    private static Process toDomain(ProcessEntity e) {
        return new Process(e.getId(), e.getClientId(), new ProcessNumber(e.getProcessNumber()), e.getSubject(),
                e.getStatus(), e.getLastCheckedAt(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
