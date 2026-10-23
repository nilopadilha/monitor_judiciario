package br.com.monitorjudicial.djen.api;

import java.time.LocalDate;

/** Filtro de busca de comunicações. {@code processNumber} somente com dígitos. */
public record DjenSearch(String processNumber, String courtCode, LocalDate from, LocalDate to) {
}
