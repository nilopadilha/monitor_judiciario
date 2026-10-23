package br.com.monitorjudicial.authentication.infrastructure.persistence;

import br.com.monitorjudicial.authentication.domain.User;
import br.com.monitorjudicial.authentication.domain.port.UserRepository;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import br.com.monitorjudicial.shared.domain.Email;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class UserPersistenceAdapter implements UserRepository {

    private final UserJpaRepository jpa;
    private final AuthPersistenceMapper mapper;

    @Override
    public User save(User user) {
        return mapper.toDomain(jpa.save(mapper.toEntity(user)));
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return jpa.findByEmail(email.value()).map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpa.existsByEmail(email.value());
    }

    @Override
    public long count() {
        return jpa.count();
    }

    @Override
    public PageResult<User> findAll(PageQuery page) {
        var result = jpa.findAll(PageRequest.of(page.page(), page.size(), Sort.by("name")));
        return PageResult.of(result.map(mapper::toDomain).getContent(), page.page(), page.size(),
                result.getTotalElements());
    }
}
