package br.com.monitorjudicial.tribunals.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TribunalTest {

    @Test
    void normalizesCodeAndState() {
        Tribunal t = Tribunal.register("tjsp", "Tribunal de Justiça de São Paulo", "sp");
        assertThat(t.code()).isEqualTo("TJSP");
        assertThat(t.state()).isEqualTo("SP");
        assertThat(t.active()).isTrue();
        t.setActive(false);
        t.rename("TJ-SP");
        assertThat(t.active()).isFalse();
        assertThat(t.name()).isEqualTo("TJ-SP");
    }

    @Test
    void rejectsInvalidState() {
        assertThatThrownBy(() -> Tribunal.register("TJXX", "X", "XX")).extracting("code").isEqualTo("invalid-state");
    }
}
