package br.com.monitorjudicial.lawyers.domain;

import br.com.monitorjudicial.shared.domain.BusinessRuleViolationException;
import br.com.monitorjudicial.shared.domain.Guard;
import java.util.Locale;
import java.util.Set;

/** Inscrição na OAB (número normalizado + seccional UF). */
public record OabNumber(String number, String uf) {

    private static final Set<String> VALID_UFS = Set.of(
            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG",
            "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO");

    public OabNumber {
        String cleanNumber = Guard.requireText(number, "invalid-oab", "Número da OAB é obrigatório.")
                .replaceAll("[^0-9A-Za-z]", "")
                .toUpperCase(Locale.ROOT);
        if (cleanNumber.isEmpty() || cleanNumber.length() > 20 || cleanNumber.chars().noneMatch(Character::isDigit)) {
            throw new BusinessRuleViolationException("invalid-oab", "Número da OAB inválido.");
        }
        String cleanUf = Guard.requireText(uf, "invalid-oab-uf", "UF da OAB é obrigatória.")
                .strip()
                .toUpperCase(Locale.ROOT);
        if (!VALID_UFS.contains(cleanUf)) {
            throw new BusinessRuleViolationException("invalid-oab-uf", "UF da OAB inválida.");
        }
        number = cleanNumber;
        uf = cleanUf;
    }

    public String formatted() {
        return "OAB/" + uf + " " + number;
    }
}
