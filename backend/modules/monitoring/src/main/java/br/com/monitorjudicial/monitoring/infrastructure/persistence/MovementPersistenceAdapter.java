package br.com.monitorjudicial.monitoring.infrastructure.persistence;

import br.com.monitorjudicial.monitoring.domain.Movement;
import br.com.monitorjudicial.monitoring.domain.port.MovementRepository;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class MovementPersistenceAdapter implements MovementRepository {

    private final MovementJpaRepository jpa;

    @Override
    public Movement save(Movement m) {
        MovementEntity e = new MovementEntity();
        e.setId(m.id());
        e.setProcessId(m.processId());
        e.setClientId(m.clientId());
        e.setPublicationId(m.publicationId());
        e.setMovementType(m.type());
        e.setDescription(m.description());
        e.setMovementDate(m.movementDate());
        e.setSource(m.source());
        e.setCreatedAt(m.createdAt());
        jpa.save(e);
        return m;
    }

    @Override
    public PageResult<Movement> findByProcess(UUID processId, PageQuery page) {
        var result = jpa.findByProcessIdOrderByMovementDateDescCreatedAtDesc(processId,
                PageRequest.of(page.page(), page.size()));
        return PageResult.of(result.map(e -> new Movement(e.getId(), e.getProcessId(), e.getClientId(),
                e.getPublicationId(), e.getMovementType(), e.getDescription(), e.getMovementDate(), e.getSource(),
                e.getCreatedAt())).getContent(), page.page(), page.size(), result.getTotalElements());
    }
}
