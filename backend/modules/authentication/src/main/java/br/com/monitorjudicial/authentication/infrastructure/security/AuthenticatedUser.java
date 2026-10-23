package br.com.monitorjudicial.authentication.infrastructure.security;

import br.com.monitorjudicial.shared.security.CurrentUser;
import br.com.monitorjudicial.shared.security.Role;
import java.util.Optional;
import java.util.UUID;

/** Principal extraído de um access token válido. */
public record AuthenticatedUser(UUID userId, String email, Role role, Optional<UUID> clientId) implements CurrentUser {
}
