package br.com.monitorjudicial.djen.api;

import java.time.LocalDate;

/** Filtro de busca de comunicações. {@code processNumber} somente com dígitos; suporta também filtro por OAB/UF. */
public record DjenSearch(String processNumber, String courtCode, LocalDate from, LocalDate to,
        String oabNumber, String oabUf, String lawyerName) {

    public DjenSearch(String processNumber, String courtCode, LocalDate from, LocalDate to) {
        this(processNumber, courtCode, from, to, null, null, null);
    }

    public static DjenSearch byOab(String oabNumber, String oabUf, LocalDate from, LocalDate to) {
        return new DjenSearch(null, null, from, to, oabNumber, oabUf, null);
    }
}
