package br.com.monitorjudicial.processes;

import br.com.monitorjudicial.processes.domain.ProcessNumber;

/** Gera números CNJ válidos para testes. */
public final class CnjNumbers {

    private CnjNumbers() {
    }

    public static String valid(String sequence, String year, String segment, String court, String origin) {
        String dd = ProcessNumber.checkDigits(sequence, year, segment, court, origin);
        return sequence + "-" + dd + "." + year + "." + segment + "." + court + "." + origin;
    }

    public static String tjsp(int sequence) {
        return valid(String.format("%07d", sequence), "2026", "8", "26", "0100");
    }
}
