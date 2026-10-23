package br.com.monitorjudicial.shared.audit;

import br.com.monitorjudicial.shared.security.CurrentUser;
import br.com.monitorjudicial.shared.security.CurrentUserProvider;
import br.com.monitorjudicial.shared.observability.CorrelationId;
import java.time.Clock;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Fachada usada pelos serviços de aplicação para registrar ações auditáveis. */
@Component
public class Auditor {

    public static final String SYSTEM_ACTOR = "system";

    private final ApplicationEventPublisher publisher;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public Auditor(ApplicationEventPublisher publisher, CurrentUserProvider currentUserProvider, Clock clock) {
        this.publisher = publisher;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    public void record(String action, String resourceType, Object resourceId, Map<String, Object> details) {
        var user = currentUserProvider.find();
        publisher.publishEvent(new AuditableActionPerformed(
                user.map(CurrentUser::userId).orElse(null),
                user.map(CurrentUser::email).orElse(SYSTEM_ACTOR),
                action,
                resourceType,
                String.valueOf(resourceId),
                details,
                CorrelationId.current().orElse(null),
                clock.instant()));
    }
}
