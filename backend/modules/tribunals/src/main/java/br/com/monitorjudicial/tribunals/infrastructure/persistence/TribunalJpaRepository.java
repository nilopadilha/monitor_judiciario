package br.com.monitorjudicial.tribunals.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface TribunalJpaRepository extends JpaRepository<TribunalEntity, UUID> {

    Optional<TribunalEntity> findByCode(String code);

    List<TribunalEntity> findByActiveOrderByCode(boolean active);

    List<TribunalEntity> findAllByOrderByCode();
}
