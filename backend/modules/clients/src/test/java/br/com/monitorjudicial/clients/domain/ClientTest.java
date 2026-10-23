package br.com.monitorjudicial.clients.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.monitorjudicial.shared.domain.Email;
import br.com.monitorjudicial.shared.domain.Phone;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ClientTest {

    private static final Instant NOW = Instant.parse("2026-10-23T10:00:00Z");
    private static final Document CNPJ = new Document("11222333000181");
    private static final Email EMAIL = new Email("juridico@horizonte.com.br");

    @Test
    void registersActiveClient() {
        Client client = Client.register("  Horizonte  ", CNPJ, EMAIL, new Phone("11987654321"), true, true, NOW);
        assertThat(client.name()).isEqualTo("Horizonte");
        assertThat(client.isActive()).isTrue();
        assertThat(client.phone()).isPresent();
        assertThat(client.createdAt()).isEqualTo(NOW);
    }

    @Test
    void whatsappRequiresPhone() {
        assertThatThrownBy(() -> Client.register("X", CNPJ, EMAIL, null, true, true, NOW))
                .extracting("code").isEqualTo("whatsapp-requires-phone");
    }

    @Test
    void atLeastOneChannel() {
        assertThatThrownBy(() -> Client.register("X", CNPJ, EMAIL, null, false, false, NOW))
                .extracting("code").isEqualTo("no-notification-channel");
    }

    @Test
    void nameLimit() {
        assertThatThrownBy(() -> Client.register("x".repeat(201), CNPJ, EMAIL, null, true, false, NOW))
                .extracting("code").isEqualTo("invalid-client-name");
    }

    @Test
    void updateDeactivateAndActivate() {
        Client client = Client.register("X", CNPJ, EMAIL, null, true, false, NOW);
        Instant later = NOW.plusSeconds(60);
        client.update("Y", new Email("novo@x.com"), new Phone("21999990000"), false, true, later);
        assertThat(client.name()).isEqualTo("Y");
        assertThat(client.notifyWhatsapp()).isTrue();
        assertThat(client.notifyEmail()).isFalse();
        assertThat(client.updatedAt()).isEqualTo(later);
        client.deactivate(later);
        assertThat(client.status()).isEqualTo(ClientStatus.INACTIVE);
        client.activate(later);
        assertThat(client.isActive()).isTrue();
    }
}
