package br.com.monitorjudicial.lawyers.application;

import br.com.monitorjudicial.lawyers.domain.Lawyer;
import br.com.monitorjudicial.lawyers.domain.PracticeArea;
import br.com.monitorjudicial.shared.domain.Phone;
import java.time.Instant;
import java.util.UUID;

public record LawyerView(
        UUID id,
        String name,
        String oabNumber,
        String oabUf,
        String oabFormatted,
        PracticeArea practiceArea,
        String practiceAreaLabel,
        String email,
        String phone,
        UUID userId,
        boolean notifyEmail,
        boolean notifyWhatsapp,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    public static LawyerView of(Lawyer l) {
        return new LawyerView(l.id(), l.name(), l.oab().number(), l.oab().uf(), l.oab().formatted(),
                l.practiceArea(), l.practiceArea().label(), l.email().value(), l.phone().map(Phone::value).orElse(null),
                l.userId().orElse(null), l.notifyEmail(), l.notifyWhatsapp(), l.active(), l.createdAt(), l.updatedAt());
    }
}
