package br.com.monitorjudicial.notifications.infrastructure.delivery;

import br.com.monitorjudicial.notifications.domain.Channel;
import br.com.monitorjudicial.notifications.domain.NotificationDeliveryException;
import br.com.monitorjudicial.notifications.domain.OutgoingMessage;
import br.com.monitorjudicial.notifications.domain.port.NotificationSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
class EmailNotificationSender implements NotificationSender {

    private final JavaMailSender mail;
    private final String from;

    EmailNotificationSender(JavaMailSender mail,
            @Value("${notifications.email.from:Monitor Judicial <nao-responda@monitorjudicial.app>}") String from) {
        this.mail = mail;
        this.from = from;
    }

    @Override
    public Channel channel() {
        return Channel.EMAIL;
    }

    @Override
    public void send(OutgoingMessage message) {
        SimpleMailMessage email = new SimpleMailMessage();
        email.setFrom(from);
        email.setTo(message.recipient());
        email.setSubject(message.subject());
        email.setText(message.body());
        try {
            mail.send(email);
        } catch (MailException e) {
            throw new NotificationDeliveryException("Servidor de e-mail recusou o envio: " + e.getMessage(), e);
        }
    }
}
