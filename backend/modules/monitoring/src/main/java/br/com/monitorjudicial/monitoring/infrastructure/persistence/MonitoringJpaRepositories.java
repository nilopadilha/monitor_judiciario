package br.com.monitorjudicial.monitoring.infrastructure.persistence;

import br.com.monitorjudicial.monitoring.domain.ExecutionStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PublicationJpaRepository extends JpaRepository<PublicationEntity, UUID>,
        JpaSpecificationExecutor<PublicationEntity> {

    interface HashRow {
        UUID getId();

        String getPublicationHash();

        String getContentFingerprint();
    }

    interface CountRow {
        UUID getProcessId();

        long getTotal();
    }

    @Query("select p.id as id, p.publicationHash as publicationHash, p.contentFingerprint as contentFingerprint "
            + "from PublicationEntity p where p.processId = :processId order by p.createdAt")
    List<HashRow> findHashes(@Param("processId") UUID processId);

    @Query("select p.processId as processId, count(p) as total from PublicationEntity p "
            + "where p.readAt is null and p.processId in :ids group by p.processId")
    List<CountRow> countUnread(@Param("ids") Collection<UUID> ids);

    @Query("select distinct p.processId from PublicationEntity p where p.readAt is null")
    List<UUID> processIdsWithUnread();
}

interface MovementJpaRepository extends JpaRepository<MovementEntity, UUID> {

    Page<MovementEntity> findByProcessIdOrderByMovementDateDescCreatedAtDesc(UUID processId, Pageable pageable);
}

interface MonitoringExecutionJpaRepository extends JpaRepository<MonitoringExecutionEntity, UUID> {

    Page<MonitoringExecutionEntity> findAllByOrderByStartedAtDesc(Pageable pageable);

    @Modifying
    @Query("update MonitoringExecutionEntity e set e.status = :aborted, e.finishedAt = :now, "
            + "e.errorMessage = 'Execução interrompida (aplicação reiniciada)' where e.status = :running")
    int abortRunning(@Param("running") ExecutionStatus running, @Param("aborted") ExecutionStatus aborted,
            @Param("now") Instant now);
}

interface MonitoringJobJpaRepository extends JpaRepository<MonitoringJobEntity, UUID> {

    List<MonitoringJobEntity> findByExecutionIdOrderByCreatedAt(UUID executionId);

    @Query(value = "select distinct on (process_id) * from monitoring_jobs where process_id in (:ids) "
            + "order by process_id, created_at desc", nativeQuery = true)
    List<MonitoringJobEntity> findLastByProcess(@Param("ids") Collection<UUID> ids);

    @Query(value = "select process_id from (select distinct on (process_id) process_id, status from monitoring_jobs "
            + "order by process_id, created_at desc) last where status = 'FAILED'", nativeQuery = true)
    List<UUID> processIdsWithFailedLastJob();
}
