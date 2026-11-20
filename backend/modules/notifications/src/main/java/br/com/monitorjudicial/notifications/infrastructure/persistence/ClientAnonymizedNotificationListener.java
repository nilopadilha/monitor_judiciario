package br.com.monitorjudicial.notifications.infrastructure.persistence;

import br.com.monitorjudicial.shared.lgpd.ClientAnonymizedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Mascara destinatários e remove conteúdo pessoal das notificações quando um titular é anonimizado (LGPD). */
@Component
@RequiredArgsConstructor
class ClientAnonymizedNotificationListener {

    private final NotificationJpaRepository notifications;

    @EventListener
    void onClientAnonymized(ClientAnonymizedEvent event) {
        notifications.anonymizeByClientId(event.clientId());
    }
}
