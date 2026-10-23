package br.com.monitorjudicial.clients.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.monitorjudicial.shared.domain.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DocumentTest {

    @Test
    void acceptsValidCpfAndFormatsIt() {
        Document cpf = new Document("529.982.247-25");
        assertThat(cpf.value()).isEqualTo("52998224725");
        assertThat(cpf.isCpf()).isTrue();
        assertThat(cpf.formatted()).isEqualTo("529.982.247-25");
        assertThat(cpf.masked()).isEqualTo("***.982.***-**");
    }

    @Test
    void acceptsValidCnpjAndFormatsIt() {
        Document cnpj = new Document("11222333000181");
        assertThat(cnpj.isCpf()).isFalse();
        assertThat(cnpj.formatted()).isEqualTo("11.222.333/0001-81");
        assertThat(cnpj.masked()).isEqualTo("11.***.***/0001-**");
    }

    @ParameterizedTest
    @ValueSource(strings = {"529.982.247-24", "111.111.111-11", "11.222.333/0001-80", "00000000000000", "123",
            "1234567890123"})
    void rejectsInvalidDocuments(String value) {
        assertThatThrownBy(() -> new Document(value))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code").isEqualTo("invalid-document");
    }
}
