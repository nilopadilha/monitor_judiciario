package br.com.monitorjudicial.clients.infrastructure.persistence;

import br.com.monitorjudicial.clients.domain.Client;
import br.com.monitorjudicial.clients.domain.ClientStatus;
import br.com.monitorjudicial.clients.domain.Document;
import br.com.monitorjudicial.clients.domain.port.ClientRepository;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class ClientPersistenceAdapter implements ClientRepository {

    private final ClientJpaRepository jpa;
    private final ClientPersistenceMapper mapper;

    @Override
    public Client save(Client client) {
        jpa.save(mapper.toEntity(client));
        return client;
    }

    @Override
    public Optional<Client> findById(UUID id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Client> findAllById(Collection<UUID> ids) {
        return jpa.findAllById(ids).stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByDocument(Document document) {
        return jpa.existsByDocument(document.value());
    }

    @Override
    public PageResult<Client> search(String text, ClientStatus status, PageQuery page) {
        Specification<ClientEntity> spec = (root, query, cb) -> cb.conjunction();
        if (text != null && !text.isBlank()) {
            String like = "%" + text.strip().toLowerCase(Locale.ROOT) + "%";
            String digits = text.replaceAll("\\D", "");
            spec = spec.and((root, query, cb) -> digits.isEmpty()
                    ? cb.like(cb.lower(root.get("name")), like)
                    : cb.or(cb.like(cb.lower(root.get("name")), like), cb.like(root.get("document"), "%" + digits + "%")));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        var result = jpa.findAll(spec, PageRequest.of(page.page(), page.size(), Sort.by("name")));
        return PageResult.of(result.map(mapper::toDomain).getContent(), page.page(), page.size(),
                result.getTotalElements());
    }
}
