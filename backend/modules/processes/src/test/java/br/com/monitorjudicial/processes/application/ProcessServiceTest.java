package br.com.monitorjudicial.processes.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.monitorjudicial.clients.api.ClientDirectory;
import br.com.monitorjudicial.clients.api.ClientSummary;
import br.com.monitorjudicial.processes.CnjNumbers;
import br.com.monitorjudicial.processes.api.ProcessMonitoringInsights;
import br.com.monitorjudicial.processes.api.ProcessMonitoringStatus;
import br.com.monitorjudicial.processes.domain.Process;
import br.com.monitorjudicial.processes.domain.ProcessNumber;
import br.com.monitorjudicial.processes.domain.ProcessStatus;
import br.com.monitorjudicial.processes.domain.port.ProcessRepository;
import br.com.monitorjudicial.processes.domain.port.ProcessRepository.ProcessFilter;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import br.com.monitorjudicial.shared.audit.Auditor;
import br.com.monitorjudicial.shared.domain.ConflictException;
import br.com.monitorjudicial.shared.domain.ResourceNotFoundException;
import br.com.monitorjudicial.shared.security.CurrentUser;
import br.com.monitorjudicial.shared.security.Role;
import br.com.monitorjudicial.tribunals.api.TribunalDirectory;
import br.com.monitorjudicial.tribunals.api.TribunalSummary;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ProcessServiceTest {

    private final ProcessRepository repository = mock(ProcessRepository.class);
    private final ClientDirectory clients = mock(ClientDirectory.class);
    private final TribunalDirectory tribunals = mock(TribunalDirectory.class);
    private final ProcessMonitoringInsights insights = mock(ProcessMonitoringInsights.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-23T10:00:00Z"), ZoneOffset.UTC);
    private final ProcessService service = new ProcessService(repository, clients, tribunals, insights,
            mock(Auditor.class), clock);

    private final UUID clientId = UUID.randomUUID();
    private final ClientSummary client = new ClientSummary(clientId, "Horizonte", "a@b.co", null, true, false, true);

    record User(UUID userId, String email, Role role, Optional<UUID> clientId) implements CurrentUser {
    }

    private final CurrentUser manager = new User(UUID.randomUUID(), "m@x.com", Role.MANAGER, Optional.empty());

    @BeforeEach
    void setUp() {
        when(clients.findById(clientId)).thenReturn(Optional.of(client));
        when(clients.findByIds(any())).thenReturn(Map.of(clientId, client));
        when(tribunals.findByCode("TJSP")).thenReturn(Optional.of(new TribunalSummary("TJSP", "TJSP", "SP", true)));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(insights.statusOf(anyCollection())).thenReturn(Map.of());
    }

    @Test
    void registersProcessDerivingCourt() {
        ProcessView view = service.register(new RegisterProcessCommand(clientId, CnjNumbers.tjsp(10), "Cobrança"));
        assertThat(view.courtCode()).isEqualTo("TJSP");
        assertThat(view.clientName()).isEqualTo("Horizonte");
        assertThat(view.situation()).isEqualTo(MonitoringSituation.MONITORED);
    }

    @Test
    void registrationRules() {
        String number = CnjNumbers.tjsp(11);
        when(repository.existsByNumber(ProcessNumber.parse(number))).thenReturn(true);
        assertThatThrownBy(() -> service.register(new RegisterProcessCommand(clientId, number, null)))
                .isInstanceOf(ConflictException.class);

        UUID unknown = UUID.randomUUID();
        when(clients.findById(unknown)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.register(new RegisterProcessCommand(unknown, CnjNumbers.tjsp(12), null)))
                .extracting("code").isEqualTo("client-not-found");

        UUID inactive = UUID.randomUUID();
        when(clients.findById(inactive)).thenReturn(Optional.of(
                new ClientSummary(inactive, "X", "x@x.com", null, true, false, false)));
        assertThatThrownBy(() -> service.register(new RegisterProcessCommand(inactive, CnjNumbers.tjsp(13), null)))
                .extracting("code").isEqualTo("client-inactive");

        String rj = CnjNumbers.valid("0000001", "2026", "8", "19", "0001");
        when(tribunals.findByCode("TJRJ")).thenReturn(Optional.of(new TribunalSummary("TJRJ", "TJRJ", "RJ", false)));
        assertThatThrownBy(() -> service.register(new RegisterProcessCommand(clientId, rj, null)))
                .extracting("code").isEqualTo("court-not-monitored");
    }

    @Test
    void customerCannotSeeOtherClientsProcess() {
        Process p = Process.register(clientId, ProcessNumber.parse(CnjNumbers.tjsp(20)), null, clock.instant());
        when(repository.findById(p.id())).thenReturn(Optional.of(p));
        var otherCustomer = new User(UUID.randomUUID(), "c@x.com", Role.CUSTOMER, Optional.of(UUID.randomUUID()));
        var owner = new User(UUID.randomUUID(), "o@x.com", Role.CUSTOMER, Optional.of(clientId));

        assertThatThrownBy(() -> service.get(p.id(), otherCustomer)).isInstanceOf(ResourceNotFoundException.class);
        assertThat(service.get(p.id(), owner).id()).isEqualTo(p.id());
    }

    @Test
    void searchAppliesCustomerScopeAndSituationFilters() {
        Process p = Process.register(clientId, ProcessNumber.parse(CnjNumbers.tjsp(30)), null, clock.instant());
        when(repository.search(any(), any())).thenReturn(PageResult.of(List.of(p), 0, 20, 1));
        when(insights.processesWithUnreadPublications()).thenReturn(Set.of(p.id()));
        when(insights.statusOf(anyCollection())).thenReturn(Map.of(p.id(), new ProcessMonitoringStatus(2, false, null)));
        var customer = new User(UUID.randomUUID(), "c@x.com", Role.CUSTOMER, Optional.of(clientId));

        var page = service.search(new ProcessSearch(UUID.randomUUID(), null, null, null,
                MonitoringSituation.NEW_PUBLICATION), PageQuery.of(0, 20), customer);

        var captor = ArgumentCaptor.forClass(ProcessFilter.class);
        verify(repository).search(captor.capture(), any());
        assertThat(captor.getValue().clientId()).isEqualTo(clientId);
        assertThat(captor.getValue().restrictTo()).containsExactly(p.id());
        assertThat(page.content().getFirst().situation()).isEqualTo(MonitoringSituation.NEW_PUBLICATION);
        assertThat(page.content().getFirst().unreadPublications()).isEqualTo(2);
    }

    @Test
    void situationFiltersForPausedAndFailed() {
        when(repository.search(any(), any())).thenReturn(PageResult.of(List.of(), 0, 20, 0));
        when(insights.processesWithFailedLastCheck()).thenReturn(Set.of());
        service.search(new ProcessSearch(null, null, null, null, MonitoringSituation.PAUSED), PageQuery.of(0, 20),
                manager);
        service.search(new ProcessSearch(null, null, null, null, MonitoringSituation.FAILED), PageQuery.of(0, 20),
                manager);
        var captor = ArgumentCaptor.forClass(ProcessFilter.class);
        verify(repository, org.mockito.Mockito.times(2)).search(captor.capture(), any());
        assertThat(captor.getAllValues().get(0).status()).isEqualTo(ProcessStatus.PAUSED);
        assertThat(captor.getAllValues().get(1).restrictTo()).isEmpty();
    }

    @Test
    void situationPriority() {
        assertThat(ProcessService.situationOf(ProcessStatus.ACTIVE, new ProcessMonitoringStatus(3, true, "e")))
                .isEqualTo(MonitoringSituation.FAILED);
        assertThat(ProcessService.situationOf(ProcessStatus.PAUSED, new ProcessMonitoringStatus(3, true, "e")))
                .isEqualTo(MonitoringSituation.PAUSED);
        assertThat(ProcessService.situationOf(ProcessStatus.ARCHIVED, ProcessMonitoringStatus.NONE))
                .isEqualTo(MonitoringSituation.ARCHIVED);
    }

    @Test
    void updateArchiveAndDirectory() {
        Process p = Process.register(clientId, ProcessNumber.parse(CnjNumbers.tjsp(40)), null, clock.instant());
        when(repository.findById(p.id())).thenReturn(Optional.of(p));
        when(repository.findAllById(any())).thenReturn(List.of(p));
        when(repository.findByStatusOrderByLastCheckedAt(ProcessStatus.ACTIVE)).thenReturn(List.of(p));

        assertThat(service.update(p.id(), "Novo", ProcessStatus.PAUSED).status()).isEqualTo(ProcessStatus.PAUSED);
        service.archive(p.id());
        assertThat(p.status()).isEqualTo(ProcessStatus.ARCHIVED);
        assertThat(service.findMonitorable()).singleElement().extracting("number").isEqualTo(p.number().digits());
        assertThat(service.findById(p.id())).isPresent();
        assertThat(service.findByIds(Set.of(p.id()))).containsKey(p.id());
        assertThat(service.findByIds(Set.of())).isEmpty();
        service.markChecked(p.id(), clock.instant());
        verify(repository).updateLastCheckedAt(p.id(), clock.instant());
    }
}
