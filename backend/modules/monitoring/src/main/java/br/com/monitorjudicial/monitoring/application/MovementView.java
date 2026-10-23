package br.com.monitorjudicial.monitoring.application;

import br.com.monitorjudicial.monitoring.domain.Movement;
import java.time.LocalDate;
import java.util.UUID;

public record MovementView(UUID id, UUID publicationId, String type, String typeLabel, String description,
        LocalDate movementDate, String source) {

    static MovementView of(Movement m) {
        return new MovementView(m.id(), m.publicationId(), m.type().name(), m.type().label(), m.description(),
                m.movementDate(), m.source().name());
    }
}
