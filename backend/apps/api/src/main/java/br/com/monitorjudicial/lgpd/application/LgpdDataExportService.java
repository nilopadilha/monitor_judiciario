package br.com.monitorjudicial.lgpd.application;

import br.com.monitorjudicial.shared.audit.Auditor;
import br.com.monitorjudicial.shared.domain.BusinessRuleViolationException;
import br.com.monitorjudicial.shared.domain.ResourceNotFoundException;
import br.com.monitorjudicial.shared.security.CurrentUser;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Art. 18, II e V LGPD: exportação estruturada dos dados pessoais do titular com trilha de auditoria. */
@Service
public class LgpdDataExportService {

    private final LgpdExportPort repository;
    private final Auditor auditor;
    private final Clock clock;

    public LgpdDataExportService(LgpdExportPort repository, Auditor auditor, Clock clock) {
        this.repository = repository;
        this.auditor = auditor;
        this.clock = clock;
    }

    @Transactional
    public ClientDataExport exportForClient(UUID clientId, CurrentUser actor) {
        if (actor.dataScope().filter(scope -> !scope.equals(clientId)).isPresent()) {
            throw new ResourceNotFoundException("cliente", clientId);
        }
        ClientDataExport data = repository.loadClientExport(clientId, clock.instant())
                .orElseThrow(() -> new ResourceNotFoundException("cliente", clientId));
        auditor.record("EXPORT_PII", "client", clientId, Map.of("processes", data.processes().size(),
                "notifications", data.notifications().size()));
        return data;
    }

    @Transactional
    public ClientDataExport exportForCurrentCustomer(CurrentUser actor) {
        UUID clientId = actor.clientId().orElseThrow(() -> new BusinessRuleViolationException("no-linked-client",
                "Usuário autenticado não possui cliente vinculado."));
        return exportForClient(clientId, actor);
    }
}
