package br.com.monitorjudicial.authentication.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.monitorjudicial.shared.domain.Email;
import br.com.monitorjudicial.shared.security.Role;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserTest {

    private static final Instant NOW = Instant.parse("2026-10-23T10:00:00Z");

    @Test
    void customerRequiresClientAndStaffMustNotHaveOne() {
        assertThatThrownBy(() -> User.create("C", new Email("c@x.com"), "hash", Role.CUSTOMER, null, NOW))
                .extracting("code").isEqualTo("customer-requires-client");
        assertThatThrownBy(() -> User.create("M", new Email("m@x.com"), "hash", Role.MANAGER, UUID.randomUUID(), NOW))
                .extracting("code").isEqualTo("client-only-for-customer");
    }

    @Test
    void createsActiveUser() {
        UUID client = UUID.randomUUID();
        User user = User.create("Cliente", new Email("c@x.com"), "hash", Role.CUSTOMER, client, NOW);
        assertThat(user.active()).isTrue();
        assertThat(user.clientId()).contains(client);
        assertThat(user.createdAt()).isEqualTo(user.updatedAt());
    }

    @Test
    void refreshTokenLifecycle() {
        RefreshToken token = RefreshToken.issue(UUID.randomUUID(), "h", UUID.randomUUID(), NOW, NOW.plusSeconds(60));
        assertThat(token.isRevoked()).isFalse();
        assertThat(token.isExpired(NOW)).isFalse();
        assertThat(token.isExpired(NOW.plusSeconds(60))).isTrue();
        UUID successor = UUID.randomUUID();
        token.rotateTo(successor, NOW);
        assertThat(token.isRevoked()).isTrue();
        assertThat(token.replacedById()).isEqualTo(successor);
        token.revoke(NOW.plusSeconds(5));
        assertThat(token.revokedAt()).isEqualTo(NOW);
    }
}
