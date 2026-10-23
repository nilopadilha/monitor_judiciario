package br.com.monitorjudicial.tribunals.domain;

import br.com.monitorjudicial.shared.domain.BusinessRuleViolationException;
import br.com.monitorjudicial.shared.domain.Guard;
import br.com.monitorjudicial.shared.domain.Ids;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Tribunal monitorado. O código segue a sigla usada pelo DJEN (TJSP, TJDFT…). */
public final class Tribunal {

    private static final Set<String> STATES = Set.of("AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT",
            "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SE", "SP", "TO");

    private final UUID id;
    private final String code;
    private String name;
    private final String state;
    private boolean active;

    public Tribunal(UUID id, String code, String name, String state, boolean active) {
        this.id = Guard.requireNonNull(id, "invalid-tribunal", "Id é obrigatório.");
        this.code = Guard.requireText(code, "invalid-tribunal-code", "Código é obrigatório.").toUpperCase(Locale.ROOT);
        this.name = Guard.requireText(name, "invalid-tribunal-name", "Nome é obrigatório.");
        String uf = state == null ? null : state.strip().toUpperCase(Locale.ROOT);
        if (uf != null && !STATES.contains(uf)) {
            throw new BusinessRuleViolationException("invalid-state", "UF inválida: " + state);
        }
        this.state = uf;
        this.active = active;
    }

    public static Tribunal register(String code, String name, String state) {
        return new Tribunal(Ids.newId(), code, name, state, true);
    }

    public void rename(String newName) {
        this.name = Guard.requireText(newName, "invalid-tribunal-name", "Nome é obrigatório.");
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public UUID id() {
        return id;
    }

    public String code() {
        return code;
    }

    public String name() {
        return name;
    }

    public String state() {
        return state;
    }

    public boolean active() {
        return active;
    }
}
