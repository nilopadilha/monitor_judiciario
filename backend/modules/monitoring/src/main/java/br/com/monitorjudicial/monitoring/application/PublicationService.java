package br.com.monitorjudicial.monitoring.application;

import br.com.monitorjudicial.monitoring.domain.Publication;
import br.com.monitorjudicial.monitoring.domain.port.PublicationRepository;
import br.com.monitorjudicial.monitoring.domain.port.PublicationRepository.PublicationFilter;
import br.com.monitorjudicial.processes.api.ProcessDirectory;
import br.com.monitorjudicial.processes.api.ProcessSummary;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import br.com.monitorjudicial.shared.audit.Auditor;
import br.com.monitorjudicial.shared.domain.ResourceNotFoundException;
import br.com.monitorjudicial.shared.security.CurrentUser;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** UC08 e UC09 — consulta de publicações (escopo do CUSTOMER) e marcação de leitura. */
@Service
public class PublicationService {

    private final PublicationRepository publications;
    private final ProcessDirectory processes;
    private final Auditor auditor;
    private final Clock clock;

    public PublicationService(PublicationRepository publications, ProcessDirectory processes, Auditor auditor,
            Clock clock) {
        this.publications = publications;
        this.processes = processes;
        this.auditor = auditor;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResult<PublicationView> search(UUID processId, LocalDate from, LocalDate to, Boolean unread,
            PageQuery page, CurrentUser user) {
        PageResult<Publication> result = publications.search(
                new PublicationFilter(user.dataScope().orElse(null), processId, from, to, unread), page);
        Map<UUID, ProcessSummary> summaries = processes.findByIds(
                result.content().stream().map(Publication::processId).distinct().toList());
        List<PublicationView> views = result.content().stream()
                .map(p -> view(p, summaries.get(p.processId())))
                .toList();
        return new PageResult<>(views, result.page(), result.size(), result.totalElements(), result.totalPages());
    }

    @Transactional(readOnly = true)
    public PublicationView get(UUID id, CurrentUser user) {
        Publication publication = load(id, user);
        return view(publication, processes.findById(publication.processId()).orElse(null));
    }

    @Transactional
    public PublicationView markRead(UUID id, CurrentUser user) {
        Publication publication = load(id, user);
        publication.markRead(clock.instant());
        publications.save(publication);
        auditor.record("MARK_READ", "publication", id, Map.of());
        return view(publication, processes.findById(publication.processId()).orElse(null));
    }

    private Publication load(UUID id, CurrentUser user) {
        return publications.findById(id)
                .filter(p -> user.dataScope().map(scope -> scope.equals(p.clientId())).orElse(true))
                .orElseThrow(() -> new ResourceNotFoundException("publicacao", id));
    }

    private static PublicationView view(Publication p, ProcessSummary process) {
        return PublicationView.of(p, process == null ? null : process.formattedNumber(),
                process == null ? null : process.courtCode());
    }
}
