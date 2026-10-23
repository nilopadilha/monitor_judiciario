package br.com.monitorjudicial.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CurrentUserTest {

    record TestUser(UUID userId, String email, Role role, Optional<UUID> clientId) implements CurrentUser {
    }

    @Test
    void staffHasNoDataScope() {
        assertThat(new TestUser(UUID.randomUUID(), "a@b.co", Role.MANAGER, Optional.empty()).dataScope()).isEmpty();
    }

    @Test
    void customerIsScopedToClient() {
        UUID client = UUID.randomUUID();
        var user = new TestUser(UUID.randomUUID(), "c@b.co", Role.CUSTOMER, Optional.of(client));
        assertThat(user.isCustomer()).isTrue();
        assertThat(user.dataScope()).contains(client);
    }

    @Test
    void customerWithoutClientSeesNothingInsteadOfEverything() {
        var user = new TestUser(UUID.randomUUID(), "c@b.co", Role.CUSTOMER, Optional.empty());
        assertThat(user.dataScope()).contains(new UUID(0, 0));
    }
}
