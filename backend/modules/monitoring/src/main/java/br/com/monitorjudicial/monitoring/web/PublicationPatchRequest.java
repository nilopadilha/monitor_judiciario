package br.com.monitorjudicial.monitoring.web;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/** Só a marcação como lida é suportada; publicações são imutáveis. */
public record PublicationPatchRequest(@NotNull @AssertTrue(message = "apenas read=true é suportado") Boolean read) {
}
