package br.com.monitorjudicial.djen.api;

import java.time.LocalDate;

/** Filtro de busca de comunicações. {@code processNumber} somente com dígitos; suporta OAB/UF e nome da parte. */
public record DjenSearch(String processNumber, String courtCode, LocalDate from, LocalDate to,
        String oabNumber, String oabUf, String lawyerName, String partyName) {

    public DjenSearch(String processNumber, String courtCode, LocalDate from, LocalDate to) {
        this(processNumber, courtCode, from, to, null, null, null, null);
    }

    public DjenSearch(String processNumber, String courtCode, LocalDate from, LocalDate to,
            String oabNumber, String oabUf, String lawyerName) {
        this(processNumber, courtCode, from, to, oabNumber, oabUf, lawyerName, null);
    }

    public static DjenSearch byOab(String oabNumber, String oabUf, LocalDate from, LocalDate to) {
        return new DjenSearch(null, null, from, to, oabNumber, oabUf, null, null);
    }

    public static DjenSearch byPartyName(String partyName, LocalDate from, LocalDate to) {
        return new DjenSearch(null, null, from, to, null, null, null, partyName);
    }
}
