package br.com.monitorjudicial.clients.application;

import br.com.monitorjudicial.clients.domain.Client;
import br.com.monitorjudicial.clients.domain.ClientStatus;
import br.com.monitorjudicial.shared.domain.Phone;
import java.time.Instant;
import java.util.UUID;

public record ClientView(UUID id, String name, String document, String email, String phone, ClientStatus status,
        boolean notifyEmail, boolean notifyWhatsapp, Instant createdAt, Instant updatedAt) {

    public static ClientView of(Client c) {
        return new ClientView(c.id(), c.name(), c.document().formatted(), c.email().value(),
                c.phone().map(Phone::value).orElse(null), c.status(), c.notifyEmail(), c.notifyWhatsapp(),
                c.createdAt(), c.updatedAt());
    }
}
