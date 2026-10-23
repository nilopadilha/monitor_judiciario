package br.com.monitorjudicial.monitoring.domain;

import java.util.UUID;

/** Resultado da comparação de uma candidata com o histórico do processo. */
public sealed interface ComparisonResult {

    /** Novidade: deve ser registrada e notificada. */
    record New(CandidatePublication candidate, String hash, String fingerprint) implements ComparisonResult {
    }

    /** Mesma comunicação já registrada (mesmo hash). */
    record Duplicate(String hash) implements ComparisonResult {
    }

    /** Mesmo conteúdo com hash diferente: retificação ou republicação, não gera notificação. */
    record Republication(String hash, UUID originalPublicationId) implements ComparisonResult {
    }
}
