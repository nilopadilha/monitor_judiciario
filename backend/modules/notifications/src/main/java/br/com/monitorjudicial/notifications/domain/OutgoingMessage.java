package br.com.monitorjudicial.notifications.domain;

import java.util.List;

/**
 * Mensagem pronta para envio. {@code templateParameters} alimenta canais que exigem template aprovado (WhatsApp).
 */
public record OutgoingMessage(Channel channel, String recipient, String subject, String body,
        List<String> templateParameters) {

    public OutgoingMessage {
        templateParameters = templateParameters == null ? List.of() : List.copyOf(templateParameters);
    }
}
