package br.com.monitorjudicial.processes.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.monitorjudicial.processes.CnjNumbers;
import br.com.monitorjudicial.shared.domain.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ProcessNumberTest {

    @ParameterizedTest
    @CsvSource({"0000001,2026,26,0100", "1234567,2019,19,0001", "9999999,2024,13,0024", "0812345,2021,07,0001"})
    void checkDigitsSatisfyIso7064Property(String sequence, String year, String court, String origin) {
        // Propriedade independente da implementação: NNNNNNN AAAA J TR OOOO DD ≡ 1 (mod 97)
        String dd = ProcessNumber.checkDigits(sequence, year, "8", court, origin);
        assertThat(new java.math.BigInteger(sequence + year + "8" + court + origin + dd)
                .mod(java.math.BigInteger.valueOf(97))).isEqualTo(java.math.BigInteger.ONE);
    }

    @Test
    void parsesAndFormats() {
        String formatted = CnjNumbers.tjsp(1);
        ProcessNumber number = ProcessNumber.parse(formatted.replaceAll("\\D", ""));
        assertThat(number.formatted()).isEqualTo(formatted);
        assertThat(number.toString()).isEqualTo(number.formatted());
        assertThat(number.year()).isEqualTo(2026);
        assertThat(number.segment()).isEqualTo(8);
    }

    @Test
    void rejectsWrongCheckDigitsWithHelpfulMessage() {
        String valid = CnjNumbers.tjsp(1234);
        String dd = valid.substring(8, 10);
        String wrong = valid.substring(0, 8) + (dd.equals("00") ? "01" : "00") + valid.substring(10);
        assertThatThrownBy(() -> ProcessNumber.parse(wrong))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("o esperado é " + dd);
    }

    @Test
    void rejectsMalformed() {
        assertThatThrownBy(() -> ProcessNumber.parse("123")).extracting("code").isEqualTo("invalid-process-number");
        assertThatThrownBy(() -> ProcessNumber.parse(null)).isInstanceOf(BusinessRuleViolationException.class);
    }

    @ParameterizedTest
    @CsvSource({"26,TJSP", "19,TJRJ", "13,TJMG", "07,TJDFT", "21,TJRS", "17,TJPE"})
    void derivesStateCourtFromNumber(String tr, String court) {
        assertThat(ProcessNumber.parse(CnjNumbers.valid("0000042", "2025", "8", tr, "0001")).courtCode())
                .isEqualTo(court);
    }

    @Test
    void federalJusticeIsOutOfScope() {
        ProcessNumber federal = ProcessNumber.parse(CnjNumbers.valid("0000042", "2025", "4", "01", "3400"));
        assertThatThrownBy(federal::courtCode).extracting("code").isEqualTo("unsupported-court");
    }
}
