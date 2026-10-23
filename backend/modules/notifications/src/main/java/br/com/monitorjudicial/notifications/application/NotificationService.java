package br.com.monitorjudicial.notifications.application;

import br.com.monitorjudicial.clients.api.ClientDirectory;
import br.com.monitorjudicial.clients.api.ClientSummary;
import br.com.monitorjudicial.monitoring.api.ProcessEvolutionDetected;
import br.com.monitorjudicial.notifications.domain.Channel;
import br.com.monitorjudicial.notifications.domain.Notification;
import br.com.monitorjudicial.notifications.domain.NotificationDeliveryException;
import br.com.monitorjudicial.notifications.domain.NotificationStatus;
import br.com.monitorjudicial.notifications.domain.port.NotificationRepository;
import br.com.monitorjudicial.notifications.domain.port.NotificationRepository.NotificationFilter;
import br.com.monitorjudicial.notifications.domain.port.NotificationSender;
import br.com.monitorjudicial.processes.api.ProcessDirectory;
import br.com.monitorjudicial.processes.api.ProcessSummary;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import br.com.monitorjudicial.shared.audit.Auditor;
import br.com.monitorjudicial.shared.domain.BusinessRuleViolationException;
import br.com.monitorjudicial.shared.domain.ResourceNotFoundException;
import br.com.monitorjudicial.shared.security.CurrentUser;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** UC06 (notificação automática, idempotente) e UC07 (envio manual). */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notifications;
    private final Map<Channel, NotificationSender> senders = new EnumMap<>(Channel.class);
    private final ClientDirectory clients;
    private final ProcessDirectory processes;
    private final NotificationMessageComposer composer;
    private final Auditor auditor;
    private final MeterRegistry meters;
    private final Clock clock;

    public NotificationService(NotificationRepository notifications, List<NotificationSender> senders,
            ClientDirectory clients, ProcessDirectory processes, NotificationMessageComposer composer,
            Auditor auditor, MeterRegistry meters, Clock clock) {
        this.notifications = notifications;
        senders.forEach(s -> this.senders.put(s.channel(), s));
        this.clients = clients;
        this.processes = processes;
        this.composer = composer;
        this.auditor = auditor;
        this.meters = meters;
        this.clock = clock;
    }

    /** Uma notificação por canal habilitado; eventos reentregues pela fila não geram duplicatas. */
    @Transactional
    public List<NotificationView> handle(ProcessEvolutionDetected event) {
        ClientSummary client = clients.findById(event.clientId()).orElse(null);
        if (client == null || !client.active()) {
            log.info("Evento {} ignorado: cliente {} inexistente ou inativo", event.eventId(), event.clientId());
            return List.of();
        }
        List<NotificationView> sent = new ArrayList<>();
        for (Channel channel : enabledChannels(client)) {
            if (notifications.existsByEventAndChannel(event.eventId(), channel)) {
                continue;
            }
            Notification notification = Notification.pending(event.eventId(), event.processId(), client.id(), channel,
                    recipient(client, channel), composer.subject(event), composer.body(event, client.name()),
                    clock.instant());
            deliver(notification, composer.templateParameters(event));
            notifications.save(notification);
            sent.add(NotificationView.of(notification, event.processNumber(), client.name()));
        }
        return sent;
    }

    @Transactional
    public NotificationView sendManual(UUID processId, Channel channel, String message) {
        ProcessSummary process = processes.findById(processId)
                .orElseThrow(() -> new ResourceNotFoundException("processo", processId));
        ClientSummary client = clients.findById(process.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("cliente", process.clientId()));
        if (!enabledChannels(client).contains(channel)) {
            throw new BusinessRuleViolationException("channel-disabled",
                    "O canal " + channel + " não está habilitado para o cliente " + client.name() + ".");
        }
        if (message == null || message.isBlank()) {
            throw new BusinessRuleViolationException("missing-message", "Informe a mensagem.");
        }
        Notification notification = Notification.pending(null, process.id(), client.id(), channel,
                recipient(client, channel), composer.manualSubject(process.formattedNumber()), message.strip(),
                clock.instant());
        deliver(notification, List.of(process.formattedNumber(), "", "Atualização", message.strip()));
        notifications.save(notification);
        auditor.record("SEND", "notification", notification.id(),
                Map.of("channel", channel.name(), "status", notification.status().name()));
        return NotificationView.of(notification, process.formattedNumber(), client.name());
    }

    @Transactional(readOnly = true)
    public PageResult<NotificationView> search(UUID processId, Channel channel, NotificationStatus status,
            PageQuery page, CurrentUser user) {
        PageResult<Notification> result = notifications.search(
                new NotificationFilter(user.dataScope().orElse(null), processId, channel, status), page);
        Map<UUID, ProcessSummary> processById = processes.findByIds(
                result.content().stream().map(Notification::processId).distinct().toList());
        Map<UUID, ClientSummary> clientById = clients.findByIds(
                result.content().stream().map(Notification::clientId).distinct().toList());
        return result.map(n -> NotificationView.of(n,
                processById.containsKey(n.processId()) ? processById.get(n.processId()).formattedNumber() : null,
                clientById.containsKey(n.clientId()) ? clientById.get(n.clientId()).name() : null));
    }

    private void deliver(Notification notification, List<String> templateParameters) {
        NotificationSender sender = senders.get(notification.channel());
        try {
            if (sender == null) {
                throw new NotificationDeliveryException("Canal " + notification.channel() + " sem provedor configurado.");
            }
            sender.send(notification.toOutgoing(templateParameters));
            notification.markSent(clock.instant());
        } catch (NotificationDeliveryException e) {
            notification.markFailed(e.getMessage());
            log.warn("Notificação {} via {} falhou: {}", notification.id(), notification.channel(), e.getMessage());
        }
        meters.counter("notifications.sent", "channel", notification.channel().name(), "status",
                notification.status().name()).increment();
    }

    private static List<Channel> enabledChannels(ClientSummary client) {
        List<Channel> channels = new ArrayList<>(2);
        if (client.notifyEmail() && client.email() != null) {
            channels.add(Channel.EMAIL);
        }
        if (client.notifyWhatsapp() && client.phone() != null) {
            channels.add(Channel.WHATSAPP);
        }
        return channels;
    }

    private static String recipient(ClientSummary client, Channel channel) {
        return channel == Channel.EMAIL ? client.email() : client.phone();
    }
}
