package br.com.monitorjudicial.notifications.domain;

import br.com.monitorjudicial.shared.domain.BusinessRuleViolationException;
import br.com.monitorjudicial.shared.domain.Ids;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Notificação a um cliente. Transições: PENDING → SENT | FAILED; FAILED → SENT em reenvio. */
public final class Notification {

    private static final int MAX_ERROR = 500;

    private final UUID id;
    private final UUID eventId;
    private final UUID processId;
    private final UUID clientId;
    private final Channel channel;
    private final String recipient;
    private final String subject;
    private final String message;
    private final Instant createdAt;
    private NotificationStatus status;
    private int attempts;
    private String errorMessage;
    private Instant sentAt;

    public Notification(UUID id, UUID eventId, UUID processId, UUID clientId, Channel channel, String recipient,
            String subject, String message, NotificationStatus status, int attempts, String errorMessage,
            Instant createdAt, Instant sentAt) {
        this.id = id;
        this.eventId = eventId;
        this.processId = processId;
        this.clientId = clientId;
        this.channel = channel;
        this.recipient = recipient;
        this.subject = subject;
        this.message = message;
        this.status = status;
        this.attempts = attempts;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.sentAt = sentAt;
    }

    public static Notification pending(UUID eventId, UUID processId, UUID clientId, Channel channel, String recipient,
            String subject, String message, Instant now) {
        if (recipient == null || recipient.isBlank()) {
            throw new BusinessRuleViolationException("missing-recipient", "Destinatário ausente para " + channel + ".");
        }
        return new Notification(Ids.newId(), eventId, processId, clientId, channel, recipient, subject, message,
                NotificationStatus.PENDING, 0, null, now, null);
    }

    public void markSent(Instant now) {
        if (status == NotificationStatus.SENT) {
            throw new IllegalStateException("Notificação já enviada");
        }
        attempts++;
        status = NotificationStatus.SENT;
        errorMessage = null;
        sentAt = now;
    }

    public void markFailed(String error) {
        if (status == NotificationStatus.SENT) {
            throw new IllegalStateException("Notificação já enviada");
        }
        attempts++;
        status = NotificationStatus.FAILED;
        String text = error == null ? "Falha desconhecida" : error;
        errorMessage = text.length() > MAX_ERROR ? text.substring(0, MAX_ERROR) : text;
    }

    public OutgoingMessage toOutgoing(java.util.List<String> templateParameters) {
        return new OutgoingMessage(channel, recipient, subject, message, templateParameters);
    }

    public UUID id() {
        return id;
    }

    public Optional<UUID> eventId() {
        return Optional.ofNullable(eventId);
    }

    public UUID processId() {
        return processId;
    }

    public UUID clientId() {
        return clientId;
    }

    public Channel channel() {
        return channel;
    }

    public String recipient() {
        return recipient;
    }

    public String subject() {
        return subject;
    }

    public String message() {
        return message;
    }

    public NotificationStatus status() {
        return status;
    }

    public int attempts() {
        return attempts;
    }

    public Optional<String> errorMessage() {
        return Optional.ofNullable(errorMessage);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Optional<Instant> sentAt() {
        return Optional.ofNullable(sentAt);
    }
}
