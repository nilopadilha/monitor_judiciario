package br.com.monitorjudicial.processes.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.monitorjudicial.processes.CnjNumbers;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProcessTest {

    private static final Instant NOW = Instant.parse("2026-10-23T10:00:00Z");

    @Test
    void registersActiveWithDerivedCourt() {
        Process p = Process.register(UUID.randomUUID(), ProcessNumber.parse(CnjNumbers.tjsp(1)), "  Cobrança ", NOW);
        assertThat(p.courtCode()).isEqualTo("TJSP");
        assertThat(p.subject()).contains("Cobrança");
        assertThat(p.isMonitorable()).isTrue();
        assertThat(p.lastCheckedAt()).isEmpty();
        p.markChecked(NOW);
        assertThat(p.lastCheckedAt()).contains(NOW);
    }

    @Test
    void pauseAndArchiveRules() {
        Process p = Process.register(UUID.randomUUID(), ProcessNumber.parse(CnjNumbers.tjsp(2)), null, NOW);
        assertThat(p.subject()).isEmpty();
        p.update("x", ProcessStatus.PAUSED, NOW);
        assertThat(p.isMonitorable()).isFalse();
        p.archive(NOW);
        assertThat(p.status()).isEqualTo(ProcessStatus.ARCHIVED);
        assertThatThrownBy(() -> p.update("x", ProcessStatus.ACTIVE, NOW)).extracting("code")
                .isEqualTo("process-archived");
        assertThatThrownBy(() -> Process.register(UUID.randomUUID(), ProcessNumber.parse(CnjNumbers.tjsp(3)),
                "x".repeat(301), NOW)).extracting("code").isEqualTo("invalid-subject");
    }
}
