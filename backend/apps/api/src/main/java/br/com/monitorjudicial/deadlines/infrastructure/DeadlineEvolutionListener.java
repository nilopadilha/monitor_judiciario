package br.com.monitorjudicial.deadlines.infrastructure;

import br.com.monitorjudicial.deadlines.application.DeadlineService;
import br.com.monitorjudicial.monitoring.api.ProcessEvolutionDetected;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Registra automaticamente prazos detectados após o commit da publicação (isolado, sem risco de rollback do job). */
@Component
@RequiredArgsConstructor
class DeadlineEvolutionListener {

    private final DeadlineService deadlines;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onEvolution(ProcessEvolutionDetected event) {
        deadlines.registerFromEvolution(event);
    }
}
