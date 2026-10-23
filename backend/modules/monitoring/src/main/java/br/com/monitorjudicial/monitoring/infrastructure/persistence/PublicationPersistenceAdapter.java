package br.com.monitorjudicial.monitoring.infrastructure.persistence;

import br.com.monitorjudicial.monitoring.domain.Publication;
import br.com.monitorjudicial.monitoring.domain.PublicationHistory;
import br.com.monitorjudicial.monitoring.domain.port.PublicationRepository;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class PublicationPersistenceAdapter implements PublicationRepository {

    private final PublicationJpaRepository jpa;

    @Override
    public Publication save(Publication p) {
        PublicationEntity e = new PublicationEntity();
        e.setId(p.id());
        e.setProcessId(p.processId());
        e.setClientId(p.clientId());
        e.setPublicationHash(p.publicationHash());
        e.setContentFingerprint(p.contentFingerprint());
        e.setDjenId(p.djenId().orElse(null));
        e.setPublicationDate(p.publicationDate());
        e.setCommunicationType(p.communicationType());
        e.setDocumentType(p.documentType());
        e.setOrganName(p.organName());
        e.setContent(p.content());
        e.setLink(p.link());
        e.setSource(p.source());
        e.setReadAt(p.readAt().orElse(null));
        e.setCreatedAt(p.createdAt());
        jpa.save(e);
        return p;
    }

    @Override
    public Optional<Publication> findById(UUID id) {
        return jpa.findById(id).map(PublicationPersistenceAdapter::toDomain);
    }

    @Override
    public PublicationHistory historyOf(UUID processId) {
        Set<String> hashes = new HashSet<>();
        Map<String, UUID> fingerprints = new HashMap<>();
        for (PublicationJpaRepository.HashRow row : jpa.findHashes(processId)) {
            hashes.add(row.getPublicationHash());
            fingerprints.putIfAbsent(row.getContentFingerprint(), row.getId());
        }
        return new PublicationHistory(hashes, fingerprints);
    }

    @Override
    public PageResult<Publication> search(PublicationFilter filter, PageQuery page) {
        Specification<PublicationEntity> spec = (root, query, cb) -> cb.conjunction();
        if (filter.clientId() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("clientId"), filter.clientId()));
        }
        if (filter.processId() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("processId"), filter.processId()));
        }
        if (filter.from() != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("publicationDate"), filter.from()));
        }
        if (filter.to() != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("publicationDate"), filter.to()));
        }
        if (filter.unread() != null) {
            spec = spec.and((root, query, cb) -> filter.unread()
                    ? cb.isNull(root.get("readAt"))
                    : cb.isNotNull(root.get("readAt")));
        }
        var result = jpa.findAll(spec, PageRequest.of(page.page(), page.size(),
                Sort.by(Sort.Order.desc("publicationDate"), Sort.Order.desc("createdAt"))));
        return PageResult.of(result.map(PublicationPersistenceAdapter::toDomain).getContent(), page.page(),
                page.size(), result.getTotalElements());
    }

    @Override
    public Map<UUID, Long> countUnreadByProcess(Collection<UUID> processIds) {
        return jpa.countUnread(processIds).stream().collect(Collectors.toMap(
                PublicationJpaRepository.CountRow::getProcessId, PublicationJpaRepository.CountRow::getTotal));
    }

    @Override
    public Set<UUID> processIdsWithUnread() {
        return Set.copyOf(jpa.processIdsWithUnread());
    }

    private static Publication toDomain(PublicationEntity e) {
        return new Publication(e.getId(), e.getProcessId(), e.getClientId(), e.getPublicationHash(),
                e.getContentFingerprint(), e.getDjenId(), e.getPublicationDate(), e.getCommunicationType(),
                e.getDocumentType(), e.getOrganName(), e.getContent(), e.getLink(), e.getSource(), e.getCreatedAt(),
                e.getReadAt());
    }
}
