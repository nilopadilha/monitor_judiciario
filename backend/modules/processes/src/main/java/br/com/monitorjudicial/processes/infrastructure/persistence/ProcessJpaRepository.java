package br.com.monitorjudicial.processes.infrastructure.persistence;

import br.com.monitorjudicial.processes.domain.ProcessStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ProcessJpaRepository extends JpaRepository<ProcessEntity, UUID>, JpaSpecificationExecutor<ProcessEntity> {

    boolean existsByProcessNumber(String processNumber);

    @Query("select p from ProcessEntity p where p.status = :status order by p.lastCheckedAt asc nulls first")
    List<ProcessEntity> findByStatusOrderByLastChecked(@Param("status") ProcessStatus status);

    @Modifying
    @Query("update ProcessEntity p set p.lastCheckedAt = :checkedAt where p.id = :id")
    int updateLastCheckedAt(@Param("id") UUID id, @Param("checkedAt") Instant checkedAt);
}
