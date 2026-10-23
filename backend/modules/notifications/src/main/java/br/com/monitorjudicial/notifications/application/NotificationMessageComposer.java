package br.com.monitorjudicial.notifications.application;

import br.com.monitorjudicial.monitoring.api.ProcessEvolutionDetected;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Component;

/** Textos das notificações, no tom definido pelo design system: fato + próximo passo, sem emoji. */
@Component
public class NotificationMessageComposer {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public String subject(ProcessEvolutionDetected e) {
        return "Nova publicação no processo " + e.processNumber();
    }

    public String body(ProcessEvolutionDetected e, String clientName) {
        return """
                Olá, %s.

                Há uma nova publicação no DJEN em %s no processo %s (%s).

                %s: %s

                Acesse o Monitor Judicial para ver o teor completo.
                """.formatted(clientName, DATE.format(e.publicationDate()), e.processNumber(), e.courtCode(),
                e.movementLabel(), e.excerpt()).strip();
    }

    /** Parâmetros do template aprovado na Meta: {{1}} processo, {{2}} data, {{3}} tipo, {{4}} trecho. */
    public List<String> templateParameters(ProcessEvolutionDetected e) {
        return List.of(e.processNumber(), DATE.format(e.publicationDate()), e.movementLabel(), e.excerpt());
    }

    public String manualSubject(String processNumber) {
        return "Atualização do processo " + processNumber;
    }
}
