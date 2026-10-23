package br.com.monitorjudicial.notifications.infrastructure.delivery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import br.com.monitorjudicial.notifications.domain.Channel;
import br.com.monitorjudicial.notifications.domain.NotificationDeliveryException;
import br.com.monitorjudicial.notifications.domain.OutgoingMessage;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SendersTest {

    private final OutgoingMessage whatsappMessage = new OutgoingMessage(Channel.WHATSAPP, "+5511987654321", "s", "b",
            List.of("1001234-83.2026.8.26.0100", "22/10/2026", "Sentença", "x".repeat(1500)));

    @Test
    void whatsappSendsApprovedTemplateWithBearerToken() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var properties = new WhatsAppProperties("https://graph.test", null, "123", "token", null, null);
        server.expect(requestTo("https://graph.test/v21.0/123/messages"))
                .andExpect(header("Authorization", "Bearer token"))
                .andExpect(jsonPath("$.to").value("5511987654321"))
                .andExpect(jsonPath("$.template.name").value("nova_publicacao"))
                .andExpect(jsonPath("$.template.components[0].parameters[3].text").value(
                        org.hamcrest.Matchers.hasLength(1000)))
                .andRespond(withSuccess());

        new WhatsAppNotificationSender(properties, builder).send(whatsappMessage);
        server.verify();
    }

    @Test
    void whatsappFailuresAreExplicit() {
        var unconfigured = new WhatsAppNotificationSender(new WhatsAppProperties(null, null, null, null, null, null),
                RestClient.builder());
        assertThat(unconfigured.channel()).isEqualTo(Channel.WHATSAPP);
        assertThatThrownBy(() -> unconfigured.send(whatsappMessage))
                .isInstanceOf(NotificationDeliveryException.class).hasMessageContaining("não configurado");

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://graph.facebook.com/v21.0/1/messages")).andRespond(withServerError());
        var sender = new WhatsAppNotificationSender(new WhatsAppProperties(null, null, "1", "t", null, null), builder);
        assertThatThrownBy(() -> sender.send(whatsappMessage)).hasMessageContaining("HTTP 500");
    }

    @Test
    void emailSendsPlainTextAndTranslatesErrors() {
        JavaMailSender mail = mock(JavaMailSender.class);
        var sender = new EmailNotificationSender(mail, "Monitor <n@x.com>");
        assertThat(sender.channel()).isEqualTo(Channel.EMAIL);
        sender.send(new OutgoingMessage(Channel.EMAIL, "a@b.co", "Assunto", "Corpo", null));
        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail).send(captor.capture());
        assertThat(captor.getValue().getTo()).containsExactly("a@b.co");
        assertThat(captor.getValue().getFrom()).isEqualTo("Monitor <n@x.com>");

        doThrow(new MailSendException("smtp down")).when(mail).send(any(SimpleMailMessage.class));
        assertThatThrownBy(() -> sender.send(new OutgoingMessage(Channel.EMAIL, "a@b.co", "s", "b", null)))
                .isInstanceOf(NotificationDeliveryException.class).hasMessageContaining("smtp down");
    }
}
