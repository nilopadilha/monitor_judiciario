package br.com.monitorjudicial.monitoring.application;

import br.com.monitorjudicial.monitoring.domain.Publication;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PublicationView(
        UUID id,
        UUID processId,
        String processNumber,
        String courtCode,
        LocalDate publicationDate,
        String communicationType,
        String documentType,
        String organName,
        String content,
        String link,
        String source,
        boolean read,
        Instant readAt,
        Instant createdAt) {

    static PublicationView of(Publication p, String processNumber, String courtCode) {
        return new PublicationView(p.id(), p.processId(), processNumber, courtCode, p.publicationDate(),
                p.communicationType(), p.documentType(), p.organName(), p.content(), p.link(), p.source().name(),
                p.readAt().isPresent(), p.readAt().orElse(null), p.createdAt());
    }
}
