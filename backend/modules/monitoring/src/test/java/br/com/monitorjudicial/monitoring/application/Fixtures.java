package br.com.monitorjudicial.monitoring.application;

import br.com.monitorjudicial.processes.api.MonitoredProcess;
import br.com.monitorjudicial.shared.security.CurrentUser;
import br.com.monitorjudicial.shared.security.Role;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

final class Fixtures {

    private Fixtures() {
    }

    record User(UUID userId, String email, Role role, Optional<UUID> clientId) implements CurrentUser {
    }

    static MonitoredProcess process(Instant lastChecked, Instant created) {
        return new MonitoredProcess(UUID.randomUUID(), UUID.randomUUID(), "10012348320268260100",
                "1001234-83.2026.8.26.0100", "TJSP", lastChecked, created);
    }

    static CurrentUser manager() {
        return new User(UUID.randomUUID(), "m@x.com", Role.MANAGER, Optional.empty());
    }

    static CurrentUser customer(UUID clientId) {
        return new User(UUID.randomUUID(), "c@x.com", Role.CUSTOMER, Optional.of(clientId));
    }
}
