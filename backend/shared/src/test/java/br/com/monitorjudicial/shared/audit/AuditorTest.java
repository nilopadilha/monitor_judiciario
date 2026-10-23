package br.com.monitorjudicial.shared.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import br.com.monitorjudicial.shared.security.CurrentUser;
import br.com.monitorjudicial.shared.security.CurrentUserProvider;
import br.com.monitorjudicial.shared.security.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class AuditorTest {

    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-23T09:00:00Z"), ZoneOffset.UTC);

    @Test
    void recordsAuthenticatedActor() {
        UUID userId = UUID.randomUUID();
        CurrentUser user = new CurrentUser() {
            public UUID userId() { return userId; }
            public String email() { return "gestor@x.com"; }
            public Role role() { return Role.MANAGER; }
            public Optional<UUID> clientId() { return Optional.empty(); }
        };
        CurrentUserProvider provider = () -> Optional.of(user);
        new Auditor(publisher, provider, clock).record("CREATE", "client", 7, Map.of("k", "v"));

        var captor = ArgumentCaptor.forClass(AuditableActionPerformed.class);
        verify(publisher).publishEvent(captor.capture());
        var event = captor.getValue();
        assertThat(event.actorId()).isEqualTo(userId);
        assertThat(event.actorEmail()).isEqualTo("gestor@x.com");
        assertThat(event.resourceId()).isEqualTo("7");
        assertThat(event.details()).containsEntry("k", "v");
        assertThat(event.occurredAt()).isEqualTo(clock.instant());
    }

    @Test
    void systemActorWhenNoUser() {
        new Auditor(publisher, Optional::empty, clock).record("RUN", "x", "1", null);
        var captor = ArgumentCaptor.forClass(AuditableActionPerformed.class);
        verify(publisher).publishEvent(captor.capture());
        assertThat(captor.getValue().actorEmail()).isEqualTo(Auditor.SYSTEM_ACTOR);
        assertThat(captor.getValue().details()).isEmpty();
    }
}
