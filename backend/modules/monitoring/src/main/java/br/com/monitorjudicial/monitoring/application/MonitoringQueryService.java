package br.com.monitorjudicial.monitoring.application;

import br.com.monitorjudicial.monitoring.application.MonitoringExecutionView.JobView;
import br.com.monitorjudicial.monitoring.domain.MonitoringExecution;
import br.com.monitorjudicial.monitoring.domain.MonitoringJob;
import br.com.monitorjudicial.monitoring.domain.port.MonitoringExecutionRepository;
import br.com.monitorjudicial.monitoring.domain.port.MonitoringJobRepository;
import br.com.monitorjudicial.processes.api.ProcessDirectory;
import br.com.monitorjudicial.processes.api.ProcessSummary;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import br.com.monitorjudicial.shared.domain.ResourceNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MonitoringQueryService {

    private final MonitoringExecutionRepository executions;
    private final MonitoringJobRepository jobs;
    private final ProcessDirectory processes;

    public MonitoringQueryService(MonitoringExecutionRepository executions, MonitoringJobRepository jobs,
            ProcessDirectory processes) {
        this.executions = executions;
        this.jobs = jobs;
        this.processes = processes;
    }

    @Transactional(readOnly = true)
    public PageResult<MonitoringExecutionView> list(PageQuery page) {
        return executions.findRecent(page).map(e -> MonitoringExecutionView.of(e, List.of()));
    }

    @Transactional(readOnly = true)
    public MonitoringExecutionView get(UUID id) {
        MonitoringExecution execution = executions.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("execucao", id));
        List<MonitoringJob> executionJobs = jobs.findByExecution(id);
        Map<UUID, ProcessSummary> numbers = processes.findByIds(
                executionJobs.stream().map(MonitoringJob::processId).toList());
        List<JobView> views = executionJobs.stream()
                .map(j -> new JobView(j.processId(),
                        numbers.containsKey(j.processId()) ? numbers.get(j.processId()).formattedNumber() : null,
                        j.status().name(), j.executionTimeMs(), j.newPublications(), j.errorMessage()))
                .toList();
        return MonitoringExecutionView.of(execution, views);
    }
}
