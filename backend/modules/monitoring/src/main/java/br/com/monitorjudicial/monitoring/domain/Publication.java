package br.com.monitorjudicial.monitoring.domain;

import br.com.monitorjudicial.shared.domain.Guard;
import br.com.monitorjudicial.shared.domain.Ids;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/** Publicação registrada. Imutável, exceto pela marcação de leitura. */
public final class Publication {

    private final UUID id;
    private final UUID processId;
    private final UUID clientId;
    private final String publicationHash;
    private final String contentFingerprint;
    private final Long djenId;
    private final LocalDate publicationDate;
    private final String communicationType;
    private final String documentType;
    private final String organName;
    private final String content;
    private final String link;
    private final PublicationSource source;
    private final Instant createdAt;
    private Instant readAt;

    public Publication(UUID id, UUID processId, UUID clientId, String publicationHash, String contentFingerprint,
            Long djenId, LocalDate publicationDate, String communicationType, String documentType, String organName,
            String content, String link, PublicationSource source, Instant createdAt, Instant readAt) {
        this.id = Guard.requireNonNull(id, "invalid-publication", "Id é obrigatório.");
        this.processId = Guard.requireNonNull(processId, "invalid-publication", "Processo é obrigatório.");
        this.clientId = Guard.requireNonNull(clientId, "invalid-publication", "Cliente é obrigatório.");
        this.publicationHash = Guard.requireText(publicationHash, "invalid-publication", "Hash é obrigatório.");
        this.contentFingerprint = Guard.requireText(contentFingerprint, "invalid-publication",
                "Fingerprint é obrigatório.");
        this.publicationDate = Guard.requireNonNull(publicationDate, "invalid-publication", "Data é obrigatória.");
        this.source = Guard.requireNonNull(source, "invalid-publication", "Fonte é obrigatória.");
        this.djenId = djenId;
        this.communicationType = communicationType;
        this.documentType = documentType;
        this.organName = organName;
        this.content = content == null ? "" : content;
        this.link = link;
        this.createdAt = createdAt;
        this.readAt = readAt;
    }

    public static Publication fromNew(ComparisonResult.New result, UUID processId, UUID clientId, Instant now) {
        CandidatePublication c = result.candidate();
        return new Publication(Ids.newId(), processId, clientId, result.hash(), result.fingerprint(), c.djenId(),
                c.publicationDate(), c.communicationType(), c.documentType(), c.organName(), c.content(), c.link(),
                c.source(), now, null);
    }

    public void markRead(Instant at) {
        if (readAt == null) {
            readAt = at;
        }
    }

    /** Trecho inicial do conteúdo para notificações e listagens. */
    public String excerpt(int maxLength) {
        String compact = content.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").strip();
        return compact.length() <= maxLength ? compact : compact.substring(0, maxLength - 1).strip() + "…";
    }

    public UUID id() {
        return id;
    }

    public UUID processId() {
        return processId;
    }

    public UUID clientId() {
        return clientId;
    }

    public String publicationHash() {
        return publicationHash;
    }

    public String contentFingerprint() {
        return contentFingerprint;
    }

    public Optional<Long> djenId() {
        return Optional.ofNullable(djenId);
    }

    public LocalDate publicationDate() {
        return publicationDate;
    }

    public String communicationType() {
        return communicationType;
    }

    public String documentType() {
        return documentType;
    }

    public String organName() {
        return organName;
    }

    public String content() {
        return content;
    }

    public String link() {
        return link;
    }

    public PublicationSource source() {
        return source;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Optional<Instant> readAt() {
        return Optional.ofNullable(readAt);
    }
}
