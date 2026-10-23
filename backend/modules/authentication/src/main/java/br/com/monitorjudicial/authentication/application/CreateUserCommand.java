package br.com.monitorjudicial.authentication.application;

import br.com.monitorjudicial.shared.security.Role;
import java.util.UUID;

public record CreateUserCommand(String name, String email, String password, Role role, UUID clientId) {
}
