package br.com.monitorjudicial.authentication.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.monitorjudicial.authentication.domain.User;
import br.com.monitorjudicial.shared.domain.Email;
import br.com.monitorjudicial.shared.security.Role;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtTokenServiceTest {

    private static final String SECRET = "test-secret-with-at-least-32-bytes!!";
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-23T10:00:00Z"), ZoneOffset.UTC);
    private final JwtProperties properties = new JwtProperties(SECRET, null, null);
    private final JwtTokenService service = new JwtTokenService(properties, clock);

    @Test
    void issuesAndParsesCustomerToken() {
        UUID client = UUID.randomUUID();
        User user = User.create("C", new Email("c@x.com"), "h", Role.CUSTOMER, client, clock.instant());
        String token = service.issue(user);

        var parsed = service.parse(token).orElseThrow();
        assertThat(parsed.userId()).isEqualTo(user.id());
        assertThat(parsed.role()).isEqualTo(Role.CUSTOMER);
        assertThat(parsed.clientId()).contains(client);
        assertThat(service.validity()).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void rejectsExpiredTamperedAndForeignTokens() {
        User user = User.create("A", new Email("a@x.com"), "h", Role.ADMIN, null, clock.instant());
        String token = service.issue(user);

        var later = new JwtTokenService(properties, Clock.offset(clock, Duration.ofMinutes(16)));
        assertThat(later.parse(token)).isEmpty();
        assertThat(service.parse(token + "x")).isEmpty();
        assertThat(service.parse("lixo")).isEmpty();
        var other = new JwtTokenService(new JwtProperties("outro-segredo-com-mais-de-32-bytes!!", null, null), clock);
        assertThat(other.parse(token)).isEmpty();
    }

    @Test
    void refusesShortSecrets() {
        assertThatThrownBy(() -> new JwtProperties("curto", null, null)).isInstanceOf(IllegalArgumentException.class);
    }
}
