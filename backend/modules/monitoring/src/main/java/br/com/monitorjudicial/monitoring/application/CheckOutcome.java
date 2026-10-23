package br.com.monitorjudicial.monitoring.application;

/** Resultado da verificação de um processo. */
record CheckOutcome(boolean success, int newPublications) {

    static CheckOutcome success(int newPublications) {
        return new CheckOutcome(true, newPublications);
    }

    static CheckOutcome failure() {
        return new CheckOutcome(false, 0);
    }
}
