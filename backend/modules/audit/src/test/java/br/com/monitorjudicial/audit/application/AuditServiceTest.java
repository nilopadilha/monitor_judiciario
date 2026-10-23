package br.com.monitorjudicial.audit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.monitorjudicial.audit.domain.AuditLog;
import br.com.monitorjudicial.audit.domain.port.AuditLogRepository;
import br.com.monitorjudicial.audit.domain.port.AuditLogRepository.AuditFilter;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import br.com.monitorjudicial.shared.audit.AuditableActionPerformed;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AuditServiceTest {

    private final AuditLogRepository repository = mock(AuditLogRepository.class);
    private final AuditService service = new AuditService(repository);

    @Test
    void persistsEventsAndSearches() {
        UUID actor = UUID.randomUUID();
        service.on(new AuditableActionPerformed(actor, "a@x.com", "CREATE", "client", "1", Map.of("k", 1), "corr",
                Instant.parse("2026-10-23T09:00:00Z")));
        var captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().actorId()).isEqualTo(actor);
        assertThat(captor.getValue().details()).containsEntry("k", 1);
        assertThat(captor.getValue().id().version()).isEqualTo(7);

        var filter = new AuditFilter("client", null, null, null, null);
        when(repository.search(filter, PageQuery.of(0, 50))).thenReturn(PageResult.of(List.of(), 0, 50, 0));
        assertThat(service.search(filter, PageQuery.of(0, 50)).totalElements()).isZero();
    }
}
