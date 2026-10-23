package br.com.monitorjudicial.monitoring.domain;

import java.util.Optional;
import java.util.UUID;

/** Serviço de domínio das fases 4 e 5 do PDF: detectar publicações novas e compará-las com as anteriores. */
public class PublicationComparisonService {

    public ComparisonResult compare(CandidatePublication candidate, PublicationHistory history) {
        String hash = candidate.effectiveHash();
        if (history.containsHash(hash)) {
            return new ComparisonResult.Duplicate(hash);
        }
        String fingerprint = candidate.fingerprint();
        Optional<UUID> original = history.publicationWithFingerprint(fingerprint);
        if (original.isPresent()) {
            return new ComparisonResult.Republication(hash, original.get());
        }
        return new ComparisonResult.New(candidate, hash, fingerprint);
    }
}
