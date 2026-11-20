package br.com.monitorjudicial.discovery.infrastructure;

import br.com.monitorjudicial.discovery.application.ProcessDiscoveryService;
import br.com.monitorjudicial.shared.discovery.ClientRegisteredEvent;
import br.com.monitorjudicial.shared.discovery.LawyerRegisteredEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Dispara descoberta automática no DJEN em Virtual Thread logo após o commit do cadastro de cliente ou advogado. */
@Component
@ConditionalOnProperty(name = "discovery.auto-on-register", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
class AutoDiscoveryEventListener {

    private static final Logger log = LoggerFactory.getLogger(AutoDiscoveryEventListener.class);

    private final ProcessDiscoveryService discovery;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onClientRegistered(ClientRegisteredEvent event) {
        Thread.ofVirtual().name("discovery-client-" + event.clientId()).start(() -> {
            try {
                var res = discovery.discoverForClient(event.clientId());
                log.info("Descoberta automática concluída para cliente {}: {} processos cadastrados",
                        event.clientId(), res.processesCreated());
            } catch (RuntimeException e) {
                log.warn("Descoberta automática falhou para cliente {}: {}", event.clientId(), e.getMessage());
            }
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onLawyerRegistered(LawyerRegisteredEvent event) {
        Thread.ofVirtual().name("discovery-lawyer-" + event.lawyerId()).start(() -> {
            try {
                var res = discovery.discoverForLawyer(event.lawyerId());
                log.info("Descoberta automática concluída para advogado {}: {} criados, {} vinculados",
                        event.lawyerId(), res.processesCreated(), res.processesLinked());
            } catch (RuntimeException e) {
                log.warn("Descoberta automática falhou para advogado {}: {}", event.lawyerId(), e.getMessage());
            }
        });
    }
}
