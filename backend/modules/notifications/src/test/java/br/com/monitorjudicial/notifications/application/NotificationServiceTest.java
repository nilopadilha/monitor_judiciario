package br.com.monitorjudicial.notifications.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.monitorjudicial.clients.api.ClientDirectory;
import br.com.monitorjudicial.clients.api.ClientSummary;
import br.com.monitorjudicial.monitoring.api.ProcessEvolutionDetected;
import br.com.monitorjudicial.notifications.domain.Channel;
import br.com.monitorjudicial.notifications.domain.Notification;
import br.com.monitorjudicial.notifications.domain.NotificationDeliveryException;
import br.com.monitorjudicial.notifications.domain.NotificationStatus;
import br.com.monitorjudicial.notifications.domain.OutgoingMessage;
import br.com.monitorjudicial.notifications.domain.port.NotificationRepository;
import br.com.monitorjudicial.notifications.domain.port.NotificationRepository.NotificationFilter;
import br.com.monitorjudicial.notifications.domain.port.NotificationSender;
import br.com.monitorjudicial.processes.api.ProcessDirectory;
import br.com.monitorjudicial.processes.api.ProcessSummary;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import br.com.monitorjudicial.shared.audit.Auditor;
import br.com.monitorjudicial.shared.domain.ResourceNotFoundException;
import br.com.monitorjudicial.shared.security.CurrentUser;
import br.com.monitorjudicial.shared.security.Role;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NotificationServiceTest {

    private final NotificationRepository repository = mock(NotificationRepository.class);
    private final NotificationSender email = mock(NotificationSender.class);
    private final NotificationSender whatsapp = mock(NotificationSender.class);
    private final ClientDirectory clients = mock(ClientDirectory.class);
    private final ProcessDirectory processes = mock(ProcessDirectory.class);
    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-23T09:00:00Z"), ZoneOffset.UTC);
    private NotificationService service;

    private final UUID clientId = UUID.randomUUID();
    private final UUID processId = UUID.randomUUID();
    private final ProcessEvolutionDetected event = new ProcessEvolutionDetected(UUID.randomUUID(), processId, clientId,
            "1001234-83.2026.8.26.0100", "TJSP", UUID.randomUUID(), LocalDate.of(2026, 10, 22), "SENTENCA", "Sentença",
            "Julgo procedente o pedido.", clock.instant());

    record User(UUID userId, String email, Role role, Optional<UUID> clientId) implements CurrentUser {
    }

    @BeforeEach
    void setUp() {
        when(email.channel()).thenReturn(Channel.EMAIL);
        when(whatsapp.channel()).thenReturn(Channel.WHATSAPP);
        service = new NotificationService(repository, List.of(email, whatsapp), clients, processes,
                new NotificationMessageComposer(), mock(Auditor.class), meters, clock);
    }

    private void client(boolean active, boolean notifyEmail, boolean notifyWhatsapp, String phone) {
        when(clients.findById(clientId)).thenReturn(Optional.of(
                new ClientSummary(clientId, "Horizonte", "juridico@horizonte.com.br", phone, notifyEmail,
                        notifyWhatsapp, active)));
    }

    @Test
    void notifiesEachEnabledChannelAndRecordsFailuresWithReason() {
        client(true, true, true, "+5511987654321");
        doThrow(new NotificationDeliveryException("WhatsApp não configurado")).when(whatsapp).send(any());

        var result = service.handle(event);

        assertThat(result).extracting(NotificationView::channel).containsExactly(Channel.EMAIL, Channel.WHATSAPP);
        assertThat(result).extracting(NotificationView::status)
                .containsExactly(NotificationStatus.SENT, NotificationStatus.FAILED);
        assertThat(result.get(1).errorMessage()).isEqualTo("WhatsApp não configurado");
        var message = ArgumentCaptor.forClass(OutgoingMessage.class);
        verify(email).send(message.capture());
        assertThat(message.getValue().subject()).isEqualTo("Nova publicação no processo 1001234-83.2026.8.26.0100");
        assertThat(message.getValue().body()).contains("22/10/2026", "Sentença: Julgo procedente", "Olá, Horizonte.")
                .doesNotContain("%s");
        verify(repository, times(2)).save(any());
        assertThat(meters.counter("notifications.sent", "channel", "WHATSAPP", "status", "FAILED").count())
                .isEqualTo(1);
    }

    @Test
    void isIdempotentForRedeliveredEvents() {
        client(true, true, false, null);
        when(repository.existsByEventAndChannel(event.eventId(), Channel.EMAIL)).thenReturn(true);
        assertThat(service.handle(event)).isEmpty();
        verify(email, never()).send(any());
    }

    @Test
    void ignoresInactiveOrMissingClients() {
        client(false, true, false, null);
        assertThat(service.handle(event)).isEmpty();
        when(clients.findById(clientId)).thenReturn(Optional.empty());
        assertThat(service.handle(event)).isEmpty();
        verify(repository, never()).save(any());
    }

    @Test
    void manualSendValidatesChannelAndMessage() {
        client(true, true, false, null);
        when(processes.findById(processId)).thenReturn(Optional.of(
                new ProcessSummary(processId, clientId, "1001234-83.2026.8.26.0100", "TJSP", null)));

        NotificationView sent = service.sendManual(processId, Channel.EMAIL, "  Audiência remarcada. ");
        assertThat(sent.status()).isEqualTo(NotificationStatus.SENT);
        assertThat(sent.subject()).isEqualTo("Atualização do processo 1001234-83.2026.8.26.0100");

        assertThatThrownBy(() -> service.sendManual(processId, Channel.WHATSAPP, "x"))
                .extracting("code").isEqualTo("channel-disabled");
        assertThatThrownBy(() -> service.sendManual(processId, Channel.EMAIL, " "))
                .extracting("code").isEqualTo("missing-message");
        assertThatThrownBy(() -> service.sendManual(UUID.randomUUID(), Channel.EMAIL, "x"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void channelWithoutProviderFails() {
        service = new NotificationService(repository, List.of(email), clients, processes,
                new NotificationMessageComposer(), mock(Auditor.class), meters, clock);
        client(true, false, true, "+5511987654321");
        assertThat(service.handle(event)).singleElement().extracting(NotificationView::status)
                .isEqualTo(NotificationStatus.FAILED);
    }

    @Test
    void searchIsScopedForCustomers() {
        Notification n = Notification.pending(event.eventId(), processId, clientId, Channel.EMAIL, "a@b.co", "s", "m",
                clock.instant());
        when(repository.search(any(), any())).thenReturn(PageResult.of(List.of(n), 0, 20, 1));
        when(processes.findByIds(anyCollection())).thenReturn(Map.of(processId,
                new ProcessSummary(processId, clientId, "1001234-83.2026.8.26.0100", "TJSP", null)));
        when(clients.findByIds(anyCollection())).thenReturn(Map.of());

        var page = service.search(null, null, null, PageQuery.of(0, 20),
                new User(UUID.randomUUID(), "c@x.com", Role.CUSTOMER, Optional.of(clientId)));

        var filter = ArgumentCaptor.forClass(NotificationFilter.class);
        verify(repository).search(filter.capture(), any());
        assertThat(filter.getValue().clientId()).isEqualTo(clientId);
        assertThat(page.content().getFirst().processNumber()).isEqualTo("1001234-83.2026.8.26.0100");
        assertThat(page.content().getFirst().clientName()).isNull();
    }
}
