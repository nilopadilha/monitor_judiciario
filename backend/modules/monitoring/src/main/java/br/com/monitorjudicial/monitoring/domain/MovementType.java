package br.com.monitorjudicial.monitoring.domain;

/** Tipos de evolução processual reconhecidos. */
public enum MovementType {
    SENTENCA("Sentença"),
    ACORDAO("Acórdão"),
    DECISAO("Decisão"),
    DESPACHO("Despacho"),
    CITACAO("Citação"),
    INTIMACAO("Intimação"),
    EDITAL("Edital"),
    OUTROS("Outros");

    private final String label;

    MovementType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
