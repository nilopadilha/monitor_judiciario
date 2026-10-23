package br.com.monitorjudicial.monitoring.domain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Hashes e fingerprints já conhecidos de um processo; atualizado à medida que novas publicações são aceitas. */
public final class PublicationHistory {

    private final Set<String> hashes;
    private final Map<String, UUID> fingerprints;

    public PublicationHistory(Set<String> hashes, Map<String, UUID> fingerprints) {
        this.hashes = new HashSet<>(hashes);
        this.fingerprints = new HashMap<>(fingerprints);
    }

    public static PublicationHistory empty() {
        return new PublicationHistory(Set.of(), Map.of());
    }

    public boolean containsHash(String hash) {
        return hashes.contains(hash);
    }

    public Optional<UUID> publicationWithFingerprint(String fingerprint) {
        return Optional.ofNullable(fingerprints.get(fingerprint));
    }

    public void accept(Publication publication) {
        hashes.add(publication.publicationHash());
        fingerprints.putIfAbsent(publication.contentFingerprint(), publication.id());
    }

    public int size() {
        return hashes.size();
    }
}
