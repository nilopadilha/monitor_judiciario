package br.com.monitorjudicial.notifications.application;

import br.com.monitorjudicial.clients.api.ClientDirectory;
import br.com.monitorjudicial.clients.api.ClientSummary;
import br.com.monitorjudicial.lawyers.api.LawyerDirectory;
import br.com.monitorjudicial.lawyers.api.LawyerSummary;
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
    private final LawyerDirectory lawyers;
    private final ProcessDirectory processes;
    private final NotificationMessageComposer composer;
    private final Auditor auditor;
    private final MeterRegistry meters;
    private final Clock clock;

    public NotificationService(NotificationRepository notifications, List<NotificationSender> senders,
            ClientDirectory clients, LawyerDirectory lawyers, ProcessDirectory processes,
            NotificationMessageComposer composer, Auditor auditor, MeterRegistry meters, Clock clock) {
        this.notifications = notifications;
        senders.forEach(s -> this.senders.put(s.channel(), s));
        this.clients = clients;
        this.lawyers = lawyers;
        this.processes = processes;
        this.composer = composer;
        this.auditor = auditor;
        this.meters = meters;
        this.clock = clock;
    }

    /** Uma notificação por canal habilitado para o cliente e alerta direto ao advogado responsável. */
    @Transactional
    public List<NotificationView> handle(ProcessEvolutionDetected event) {
        ClientSummary client = clients.findById(event.clientId()).orElse(null);
        List<NotificationView> sent = new ArrayList<>();
        if (client != null && client.active()) {
            for (Channel channel : enabledChannels(client)) {
                if (notifications.existsByEventAndChannel(event.eventId(), channel)) {
                    continue;
                }
                Notification notification = Notification.pending(event.eventId(), event.processId(), client.id(),
                        channel, recipient(client, channel), composer.subject(event),
                        composer.body(event, client.name()), clock.instant());
                deliver(notification, composer.templateParameters(event));
                notifications.save(notification);
                sent.add(NotificationView.of(notification, event.processNumber(), client.name()));
            }
        } else {
            log.info("Notificação ao cliente ignorada no evento {}: cliente {} inexistente ou inativo",
                    event.eventId(), event.clientId());
        }
        notifyResponsibleLawyer(event, client == null ? "Cliente" : client.name());
        return sent;
    }

    private void notifyResponsibleLawyer(ProcessEvolutionDetected event, String clientName) {
        if (event.lawyerId() == null) {
            return;
        }
        LawyerSummary lawyer = lawyers.findById(event.lawyerId()).filter(LawyerSummary::active).orElse(null);
        if (lawyer == null) {
            return;
        }
        String subject = "[Advogado] " + composer.subject(event) + " — " + clientName;
        String body = composer.body(event, lawyer.name() + " (Cliente: " + clientName + ")");
        if (lawyer.notifyEmail() && lawyer.email() != null && !lawyer.email().isBlank()) {
            sendToLawyer(Channel.EMAIL, lawyer.email(), subject, body, composer.templateParameters(event));
        }
        if (lawyer.notifyWhatsapp() && lawyer.phone() != null && !lawyer.phone().isBlank()) {
            sendToLawyer(Channel.WHATSAPP, lawyer.phone(), subject, body, composer.templateParameters(event));
        }
    }

    private void sendToLawyer(Channel channel, String recipient, String subject, String body,
            List<String> templateParams) {
        NotificationSender sender = senders.get(channel);
        if (sender == null) {
            return;
        }
        try {
            sender.send(new OutgoingMessage(channel, recipient, subject, body, templateParams));
            meters.counter("notifications.lawyer.sent", "channel", channel.name(), "outcome", "success").increment();
        } catch (NotificationDeliveryException e) {
            meters.counter("notifications.lawyer.sent", "channel", channel.name(), "outcome", "failed").increment();
            log.warn("Falha ao alertar advogado via {}: {}", channel, e.getMessage());
        }
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
