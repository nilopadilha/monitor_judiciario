package br.com.monitorjudicial.monitoring.infrastructure.persistence;

import br.com.monitorjudicial.monitoring.domain.MovementType;
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
@Table(name = "movements")
@Getter
@Setter
@NoArgsConstructor
public class MovementEntity {

    @Id
    private UUID id;

    @Column(name = "process_id", nullable = false)
    private UUID processId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "publication_id")
    private UUID publicationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false)
    private MovementType movementType;

    @Column(nullable = false)
    private String description;

    @Column(name = "movement_date", nullable = false)
    private LocalDate movementDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PublicationSource source;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
