package br.com.monitorjudicial.monitoring.domain;

import br.com.monitorjudicial.shared.domain.Ids;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Evolução processual detectada. */
public record Movement(
        UUID id,
        UUID processId,
        UUID clientId,
        UUID publicationId,
        MovementType type,
        String description,
        LocalDate movementDate,
        PublicationSource source,
        Instant createdAt) {

    public static Movement fromPublication(Publication publication, MovementType type, Instant now) {
        String prefix = type == MovementType.OUTROS ? "" : type.label() + ": ";
        return new Movement(Ids.newId(), publication.processId(), publication.clientId(), publication.id(), type,
                prefix + publication.excerpt(480), publication.publicationDate(), publication.source(), now);
    }
}
