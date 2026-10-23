package br.com.monitorjudicial.monitoring.application;

import br.com.monitorjudicial.monitoring.domain.JobStatus;
import br.com.monitorjudicial.monitoring.domain.MonitoringJob;
import br.com.monitorjudicial.monitoring.domain.port.MonitoringJobRepository;
import br.com.monitorjudicial.monitoring.domain.port.PublicationRepository;
import br.com.monitorjudicial.processes.api.ProcessMonitoringInsights;
import br.com.monitorjudicial.processes.api.ProcessMonitoringStatus;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Implementa a porta definida por processes (inversão de dependência). */
@Component
class ProcessMonitoringInsightsAdapter implements ProcessMonitoringInsights {

    private final PublicationRepository publications;
    private final MonitoringJobRepository jobs;

    ProcessMonitoringInsightsAdapter(PublicationRepository publications, MonitoringJobRepository jobs) {
        this.publications = publications;
        this.jobs = jobs;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, ProcessMonitoringStatus> statusOf(Collection<UUID> processIds) {
        if (processIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Long> unread = publications.countUnreadByProcess(processIds);
        Map<UUID, MonitoringJob> lastJobs = jobs.lastJobByProcess(processIds);
        Map<UUID, ProcessMonitoringStatus> result = new HashMap<>();
        for (UUID id : processIds) {
            MonitoringJob job = lastJobs.get(id);
            boolean failed = job != null && job.status() == JobStatus.FAILED;
            result.put(id, new ProcessMonitoringStatus(unread.getOrDefault(id, 0L), failed,
                    failed ? job.errorMessage() : null));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> processesWithUnreadPublications() {
        return publications.processIdsWithUnread();
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> processesWithFailedLastCheck() {
        return jobs.processIdsWithFailedLastJob();
    }
}
