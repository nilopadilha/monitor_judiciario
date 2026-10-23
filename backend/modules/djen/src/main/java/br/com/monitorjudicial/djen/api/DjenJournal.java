package br.com.monitorjudicial.djen.api;

import java.time.LocalDate;

/** Caderno diário de um tribunal ({@code medium}: E = eletrônico, D = diário). */
public record DjenJournal(String courtCode, LocalDate date, String medium, String downloadUrl, int totalCommunications) {
}
