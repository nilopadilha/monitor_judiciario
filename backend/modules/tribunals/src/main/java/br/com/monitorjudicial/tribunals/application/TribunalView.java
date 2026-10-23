package br.com.monitorjudicial.tribunals.application;

import br.com.monitorjudicial.tribunals.domain.Tribunal;

public record TribunalView(String code, String name, String state, boolean active) {

    public static TribunalView of(Tribunal t) {
        return new TribunalView(t.code(), t.name(), t.state(), t.active());
    }
}
