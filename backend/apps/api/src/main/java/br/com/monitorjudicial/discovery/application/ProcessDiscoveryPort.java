package br.com.monitorjudicial.discovery.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProcessDiscoveryPort {

    Optional<ExistingProcess> findByDigits(String processDigits);

    void assignLawyer(UUID processId, UUID lawyerId, String practiceArea);

    List<ActiveClient> findActiveClients();

    record ExistingProcess(UUID id, UUID clientId, UUID lawyerId, String formattedNumber) {
    }

    record ActiveClient(UUID id, String name) {
    }
}
