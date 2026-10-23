package br.com.monitorjudicial.authentication.application;

import br.com.monitorjudicial.authentication.domain.User;
import br.com.monitorjudicial.shared.security.Role;
import java.time.Instant;
import java.util.UUID;

/** Representação de usuário sem dados sensíveis. */
public record UserView(UUID id, String name, String email, Role role, UUID clientId, boolean active,
        Instant createdAt) {

    public static UserView of(User user) {
        return new UserView(user.id(), user.name(), user.email().value(), user.role(),
                user.clientId().orElse(null), user.active(), user.createdAt());
    }
}
