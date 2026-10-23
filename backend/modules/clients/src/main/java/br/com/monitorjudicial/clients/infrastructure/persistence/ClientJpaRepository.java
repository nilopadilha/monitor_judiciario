package br.com.monitorjudicial.clients.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface ClientJpaRepository extends JpaRepository<ClientEntity, UUID>, JpaSpecificationExecutor<ClientEntity> {

    boolean existsByDocument(String document);
}
