package br.com.monitorjudicial.monitoring.domain.port;

import br.com.monitorjudicial.monitoring.domain.Publication;
import br.com.monitorjudicial.monitoring.domain.PublicationHistory;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface PublicationRepository {

    Publication save(Publication publication);

    Optional<Publication> findById(UUID id);

    PublicationHistory historyOf(UUID processId);

    PageResult<Publication> search(PublicationFilter filter, PageQuery page);

    Map<UUID, Long> countUnreadByProcess(Collection<UUID> processIds);

    Set<UUID> processIdsWithUnread();

    record PublicationFilter(UUID clientId, UUID processId, LocalDate from, LocalDate to, Boolean unread) {
    }
}
