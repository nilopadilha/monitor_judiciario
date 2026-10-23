package br.com.monitorjudicial.notifications.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationTest {

    private static final Instant NOW = Instant.parse("2026-10-23T09:00:00Z");

    private Notification pending() {
        return Notification.pending(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Channel.EMAIL, "a@b.co",
                "Assunto", "Corpo", NOW);
    }

    @Test
    void failedThenResentSuccessfully() {
        Notification n = pending();
        n.markFailed("x".repeat(800));
        assertThat(n.status()).isEqualTo(NotificationStatus.FAILED);
        assertThat(n.errorMessage()).get().asString().hasSize(500);
        n.markSent(NOW);
        assertThat(n.status()).isEqualTo(NotificationStatus.SENT);
        assertThat(n.errorMessage()).isEmpty();
        assertThat(n.attempts()).isEqualTo(2);
        assertThat(n.sentAt()).contains(NOW);
        assertThatThrownBy(() -> n.markSent(NOW)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> n.markFailed("x")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void requiresRecipientAndBuildsOutgoingMessage() {
        assertThatThrownBy(() -> Notification.pending(null, UUID.randomUUID(), UUID.randomUUID(), Channel.WHATSAPP,
                " ", "s", "m", NOW)).extracting("code").isEqualTo("missing-recipient");
        OutgoingMessage message = pending().toOutgoing(List.of("p1"));
        assertThat(message.recipient()).isEqualTo("a@b.co");
        assertThat(message.templateParameters()).containsExactly("p1");
        assertThat(new OutgoingMessage(Channel.EMAIL, "r", "s", "b", null).templateParameters()).isEmpty();
        pending().markFailed(null);
    }
}
