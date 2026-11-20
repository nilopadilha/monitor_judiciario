package br.com.monitorjudicial.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PiiMaskerTest {

    @Test
    void masksEmailsSafely() {
        assertThat(PiiMasker.maskEmail("admin@monitorjudicial.local")).isEqualTo("a***@monitorjudicial.local");
        assertThat(PiiMasker.maskEmail("  maria@x.com ")).isEqualTo("m***@x.com");
        assertThat(PiiMasker.maskEmail("invalido")).isEqualTo("***");
        assertThat(PiiMasker.maskEmail(null)).isEqualTo("***");
        assertThat(PiiMasker.maskEmail("   ")).isEqualTo("***");
    }

    @Test
    void masksPhonesSafely() {
        assertThat(PiiMasker.maskPhone("+5511987654321")).isEqualTo("+5511*****4321");
        assertThat(PiiMasker.maskPhone("99991234")).isEqualTo("*****1234");
        assertThat(PiiMasker.maskPhone("1234")).isEqualTo("****");
        assertThat(PiiMasker.maskPhone(null)).isEqualTo("***");
        assertThat(PiiMasker.maskPhone(" ")).isEqualTo("***");
    }
}
