package br.com.monitorjudicial.monitoring.infrastructure.persistence;

import br.com.monitorjudicial.monitoring.domain.PublicationSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "publications")
@Getter
@Setter
@NoArgsConstructor
public class PublicationEntity {

    @Id
    private UUID id;

    @Column(name = "process_id", nullable = false)
    private UUID processId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "publication_hash", nullable = false)
    private String publicationHash;

    @Column(name = "content_fingerprint", nullable = false)
    private String contentFingerprint;

    @Column(name = "djen_id")
    private Long djenId;

    @Column(name = "publication_date", nullable = false)
    private LocalDate publicationDate;

    @Column(name = "communication_type")
    private String communicationType;

    @Column(name = "document_type")
    private String documentType;

    @Column(name = "organ_name")
    private String organName;

    @Column(nullable = false)
    private String content;

    private String link;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PublicationSource source;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
