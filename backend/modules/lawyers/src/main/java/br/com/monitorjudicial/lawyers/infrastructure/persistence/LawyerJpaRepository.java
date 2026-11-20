package br.com.monitorjudicial.lawyers.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface LawyerJpaRepository extends JpaRepository<LawyerEntity, UUID>, JpaSpecificationExecutor<LawyerEntity> {

    boolean existsByOabNumberAndOabUf(String oabNumber, String oabUf);

    List<LawyerEntity> findByActiveTrueOrderByNameAsc();
}
