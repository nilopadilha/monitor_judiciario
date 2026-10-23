package br.com.monitorjudicial.tribunals.infrastructure.persistence;

import br.com.monitorjudicial.tribunals.domain.Tribunal;
import br.com.monitorjudicial.tribunals.domain.port.TribunalRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class TribunalPersistenceAdapter implements TribunalRepository {

    private final TribunalJpaRepository jpa;

    @Override
    public Tribunal save(Tribunal tribunal) {
        TribunalEntity e = new TribunalEntity();
        e.setId(tribunal.id());
        e.setCode(tribunal.code());
        e.setName(tribunal.name());
        e.setState(tribunal.state());
        e.setActive(tribunal.active());
        jpa.save(e);
        return tribunal;
    }

    @Override
    public Optional<Tribunal> findByCode(String code) {
        return jpa.findByCode(code).map(TribunalPersistenceAdapter::toDomain);
    }

    @Override
    public List<Tribunal> findAll(Boolean active) {
        var rows = active == null ? jpa.findAllByOrderByCode() : jpa.findByActiveOrderByCode(active);
        return rows.stream().map(TribunalPersistenceAdapter::toDomain).toList();
    }

    private static Tribunal toDomain(TribunalEntity e) {
        return new Tribunal(e.getId(), e.getCode(), e.getName(), e.getState(), e.isActive());
    }
}
