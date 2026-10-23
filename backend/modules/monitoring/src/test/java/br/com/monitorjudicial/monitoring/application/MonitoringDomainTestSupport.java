package br.com.monitorjudicial.monitoring.application;

import br.com.monitorjudicial.monitoring.domain.CandidatePublication;
import br.com.monitorjudicial.monitoring.domain.PublicationSource;
import java.time.LocalDate;

final class MonitoringDomainTestSupport {

    private MonitoringDomainTestSupport() {
    }

    static CandidatePublication candidate(String hash) {
        return new CandidatePublication(hash, 1L, LocalDate.of(2026, 10, 22), "Intimação", "Decisão", "Vara",
                "Defiro a tutela de urgência.", null, PublicationSource.DJEN);
    }
}
