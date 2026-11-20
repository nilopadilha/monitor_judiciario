package br.com.monitorjudicial.deadlines.infrastructure;

import br.com.monitorjudicial.deadlines.application.DeadlineAlertNotifier;
import br.com.monitorjudicial.deadlines.application.DeadlinePort;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
class DeadlineEmailAlertNotifier implements DeadlineAlertNotifier {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JavaMailSender mail;
    private final String from;

    DeadlineEmailAlertNotifier(JavaMailSender mail,
            @Value("${notifications.email.from:Monitor Judicial <nao-responda@monitorjudicial.app>}") String from) {
        this.mail = mail;
        this.from = from;
    }

    @Override
    public void notifyLawyer(DeadlinePort.DueDeadlineAlert alert, long daysRemaining) {
        String urgencyText = daysRemaining < 0
                ? "VENCIDO há " + Math.abs(daysRemaining) + " dia(s)"
                : daysRemaining == 0 ? "VENCE HOJE" : "vence em " + daysRemaining + " dia(s)";
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(alert.lawyerEmail());
        msg.setSubject("[Prazo " + urgencyText + "] " + alert.processNumber() + " — " + alert.title());
        msg.setText("""
                Olá, %s.

                Lembrete de prazo processual (%s):
                - Processo: %s
                - Cliente: %s
                - Prazo: %s
                - Data fatal: %s

                Acesse o Monitor Judicial para acompanhar e marcar o prazo como cumprido.
                """.formatted(alert.lawyerName(), urgencyText, alert.processNumber(), alert.clientName(),
                alert.title(), DATE.format(alert.dueDate())).strip());
        mail.send(msg);
    }
}
