package br.com.monitorjudicial.clients.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.monitorjudicial.clients.domain.Client;
import br.com.monitorjudicial.clients.domain.ClientStatus;
import br.com.monitorjudicial.clients.domain.Document;
import br.com.monitorjudicial.clients.domain.LegalBasis;
import br.com.monitorjudicial.clients.domain.port.ClientRepository;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import br.com.monitorjudicial.shared.audit.Auditor;
import br.com.monitorjudicial.shared.domain.ConflictException;
import br.com.monitorjudicial.shared.domain.Email;
import br.com.monitorjudicial.shared.domain.ResourceNotFoundException;
import br.com.monitorjudicial.shared.lgpd.ClientAnonymizedEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class ClientServiceTest {

    private final ClientRepository repository = mock(ClientRepository.class);
    private final Auditor auditor = mock(Auditor.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-23T10:00:00Z"), ZoneOffset.UTC);
    private final ClientService service = new ClientService(repository, auditor, events, clock);

    private final ClientCommand command = new ClientCommand("Horizonte", "11.222.333/0001-81",
            "juridico@horizonte.com.br", "11987654321", true, true);

    @Test
    void createsClientAndAuditsWithMaskedDocument() {
        ClientView view = service.create(command);
        assertThat(view.document()).isEqualTo("11.222.333/0001-81");
        assertThat(view.documentMasked()).isEqualTo("11.***.***/0001-**");
        assertThat(view.legalBasis()).isEqualTo(LegalBasis.CONTRACT_EXECUTION);
        assertThat(view.phone()).isEqualTo("+5511987654321");
        assertThat(view.status()).isEqualTo(ClientStatus.ACTIVE);
        verify(repository).save(any(Client.class));
        verify(auditor).record(eq("CREATE"), eq("client"), eq(view.id()),
                eq(java.util.Map.of("document", "11.***.***/0001-**", "legalBasis", "CONTRACT_EXECUTION")));
    }

    @Test
    void rejectsDuplicateDocument() {
        when(repository.existsByDocument(new Document("11222333000181"))).thenReturn(true);
        assertThatThrownBy(() -> service.create(command)).isInstanceOf(ConflictException.class);
    }

    @Test
    void updatesAndDeactivates() {
        Client existing = existing();
        when(repository.findById(existing.id())).thenReturn(Optional.of(existing));
        ClientView updated = service.update(existing.id(),
                new ClientCommand("Novo nome", "ignored", "novo@x.com", " ", true, false));
        assertThat(updated.name()).isEqualTo("Novo nome");
        assertThat(updated.phone()).isNull();
        service.deactivate(existing.id());
        assertThat(service.get(existing.id()).status()).isEqualTo(ClientStatus.INACTIVE);
        verify(auditor).record(eq("DEACTIVATE"), eq("client"), eq(existing.id()), anyMap());
    }

    @Test
    void missingClientIsNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(id)).isInstanceOf(ResourceNotFoundException.class);
        assertThat(service.findById(id)).isEmpty();
    }

    @Test
    void directoryExposesSummaries() {
        Client existing = existing();
        when(repository.findById(existing.id())).thenReturn(Optional.of(existing));
        when(repository.findAllById(Set.of(existing.id()))).thenReturn(List.of(existing));
        assertThat(service.findById(existing.id())).get().extracting("active").isEqualTo(true);
        assertThat(service.findByIds(Set.of(existing.id()))).containsKey(existing.id());
        assertThat(service.findByIds(Set.of())).isEmpty();
    }

    @Test
    void searchMapsViewsWithMaskedDocumentForMinimization() {
        Client existing = existing();
        when(repository.search("hor", null, PageQuery.of(0, 20)))
                .thenReturn(PageResult.of(List.of(existing), 0, 20, 1));
        var page = service.search("hor", null, PageQuery.of(0, 20));
        assertThat(page.content()).extracting(ClientView::name).containsExactly("Horizonte");
        assertThat(page.content().getFirst().document()).isEqualTo("11.***.***/0001-**");
    }

    @Test
    void anonymizesAndPublishesEventAndUpdatesPreferences() {
        Client existing = existing();
        when(repository.findById(existing.id())).thenReturn(Optional.of(existing));

        ClientView prefs = service.updateNotificationPreferences(existing.id(), true, false);
        assertThat(prefs.notifyEmail()).isTrue();
        assertThat(prefs.notifyWhatsapp()).isFalse();

        ClientView anonymized = service.anonymize(existing.id());
        assertThat(anonymized.status()).isEqualTo(ClientStatus.ANONYMIZED);
        verify(events).publishEvent(new ClientAnonymizedEvent(existing.id(), clock.instant()));
        verify(auditor).record(eq("ANONYMIZE"), eq("client"), eq(existing.id()), anyMap());
    }

    private Client existing() {
        return Client.register("Horizonte", new Document("11222333000181"), new Email("a@b.co"), null, true, false,
                clock.instant());
    }
}
