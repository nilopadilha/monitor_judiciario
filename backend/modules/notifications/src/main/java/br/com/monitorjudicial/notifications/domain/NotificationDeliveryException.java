package br.com.monitorjudicial.notifications.domain;

/** O provedor recusou ou não pôde entregar a mensagem. A mensagem é exibida ao usuário, sem dados sensíveis. */
public class NotificationDeliveryException extends RuntimeException {

    public NotificationDeliveryException(String message) {
        super(message);
    }

    public NotificationDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
