package br.com.monitorjudicial.lawyers.domain;

/** Áreas de atuação jurídica do escritório. */
public enum PracticeArea {
    CIVEL("Cível"),
    TRABALHISTA("Trabalhista"),
    TRIBUTARIO("Tributário"),
    FAMILIA("Família e Sucessões"),
    CRIMINAL("Criminal"),
    PREVIDENCIARIO("Previdenciário"),
    EMPRESARIAL("Empresarial"),
    CONSUMIDOR("Direito do Consumidor"),
    ADMINISTRATIVO("Administrativo"),
    OUTROS("Outros");

    private final String label;

    PracticeArea(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
