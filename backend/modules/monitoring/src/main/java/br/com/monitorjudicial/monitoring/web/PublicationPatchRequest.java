package br.com.monitorjudicial.monitoring.web;

import jakarta.validation.constraints.NotNull;

/** Só a marcação como lida (read=true) é suportada; publicações são imutáveis (RF-025 / RN-28). */
public record PublicationPatchRequest(@NotNull Boolean read) {
}
