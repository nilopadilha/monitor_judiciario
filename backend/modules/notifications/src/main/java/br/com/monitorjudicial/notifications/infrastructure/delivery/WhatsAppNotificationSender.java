package br.com.monitorjudicial.notifications.infrastructure.delivery;

import br.com.monitorjudicial.notifications.domain.Channel;
import br.com.monitorjudicial.notifications.domain.NotificationDeliveryException;
import br.com.monitorjudicial.notifications.domain.OutgoingMessage;
import br.com.monitorjudicial.notifications.domain.port.NotificationSender;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Envio por template aprovado (fora da janela de 24 h a Meta só aceita templates — risco R5).
 */
@Component
@EnableConfigurationProperties(WhatsAppProperties.class)
class WhatsAppNotificationSender implements NotificationSender {

    private final WhatsAppProperties properties;
    private final RestClient http;

    WhatsAppNotificationSender(WhatsAppProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.http = builder.baseUrl(properties.baseUrl()).build();
    }

    @Override
    public Channel channel() {
        return Channel.WHATSAPP;
    }

    @Override
    public void send(OutgoingMessage message) {
        if (!properties.configured()) {
            throw new NotificationDeliveryException(
                    "WhatsApp não configurado: defina NOTIFICATIONS_WHATSAPP_PHONE_NUMBER_ID e ACCESS_TOKEN.");
        }
        List<Map<String, String>> parameters = message.templateParameters().stream()
                .map(value -> Map.of("type", "text", "text", truncate(value)))
                .toList();
        Map<String, Object> body = Map.of(
                "messaging_product", "whatsapp",
                "to", message.recipient().replace("+", ""),
                "type", "template",
                "template", Map.of(
                        "name", properties.templateName(),
                        "language", Map.of("code", properties.templateLanguage()),
                        "components", List.of(Map.of("type", "body", "parameters", parameters))));
        try {
            http.post()
                    .uri("/{version}/{phone}/messages", properties.apiVersion(), properties.phoneNumberId())
                    .headers(h -> h.setBearerAuth(properties.accessToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw new NotificationDeliveryException(
                    "WhatsApp recusou o envio (HTTP " + e.getStatusCode().value() + ").", e);
        } catch (RestClientException e) {
            throw new NotificationDeliveryException("WhatsApp indisponível: " + e.getMessage(), e);
        }
    }

    /** A Meta limita parâmetros de template a 1024 caracteres. */
    private static String truncate(String value) {
        String text = value == null || value.isBlank() ? "-" : value;
        return text.length() > 1000 ? text.substring(0, 999) + "…" : text;
    }
}
