package br.com.monitorjudicial.monitoring.domain;

import java.time.LocalDate;

/** Comunicação encontrada na fonte, ainda não comparada com o histórico. */
public record CandidatePublication(
        String publicationHash,
        Long djenId,
        LocalDate publicationDate,
        String communicationType,
        String documentType,
        String organName,
        String content,
        String link,
        PublicationSource source) {

    public String fingerprint() {
        return ContentFingerprint.of(content);
    }

    /** Hash de identificação: o fornecido pela fonte ou, na ausência, o fingerprint do conteúdo. */
    public String effectiveHash() {
        return publicationHash == null || publicationHash.isBlank() ? fingerprint() : publicationHash;
    }
}
