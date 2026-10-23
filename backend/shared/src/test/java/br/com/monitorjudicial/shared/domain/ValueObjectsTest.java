package br.com.monitorjudicial.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ValueObjectsTest {

    @Test
    void emailIsNormalizedToLowerCase() {
        assertThat(new Email("  Juridico@Horizonte.COM.br ").value()).isEqualTo("juridico@horizonte.com.br");
        assertThat(new Email("a@b.co").toString()).isEqualTo("a@b.co");
    }

    @ParameterizedTest
    @ValueSource(strings = {"sem-arroba", "a@b", "a b@c.com", "@dominio.com"})
    void rejectsInvalidEmails(String value) {
        assertThatThrownBy(() -> new Email(value))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code").isEqualTo("invalid-email");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void emailIsRequired(String value) {
        assertThatThrownBy(() -> new Email(value)).isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void brazilianPhonesReceiveCountryCode() {
        assertThat(new Phone("(11) 98765-4321").value()).isEqualTo("+5511987654321");
        assertThat(new Phone("11 3333-4444").value()).isEqualTo("+551133334444");
        assertThat(new Phone("+55 21 99999-0000").toString()).isEqualTo("+5521999990000");
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "1234567890123456"})
    void rejectsInvalidPhones(String value) {
        assertThatThrownBy(() -> new Phone(value)).isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void guardTrimsTextAndRejectsBlankOrNull() {
        assertThat(Guard.requireText("  texto ", "c", "m")).isEqualTo("texto");
        assertThatThrownBy(() -> Guard.requireText("  ", "code", "mensagem"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("mensagem");
        assertThatThrownBy(() -> Guard.requireNonNull(null, "code", "nulo")).hasMessage("nulo");
        assertThat(Guard.requireNonNull(1, "c", "m")).isEqualTo(1);
    }

    @Test
    void exceptionsCarryStableCodes() {
        assertThat(new ResourceNotFoundException("cliente", 42).code()).isEqualTo("cliente-not-found");
        assertThat(new ResourceNotFoundException("cliente", 42)).hasMessage("cliente não encontrado: 42");
        assertThat(new ConflictException("dup", "x").code()).isEqualTo("dup");
        assertThat(new UnauthorizedException("u", "x").code()).isEqualTo("u");
    }
}
