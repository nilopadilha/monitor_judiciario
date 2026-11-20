package br.com.monitorjudicial.deadlines.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DeadlineCalculatorTest {

    @Test
    void addsBusinessDaysSkippingWeekends() {
        // Sexta-feira 20/11/2026 + 5 dias úteis → sexta-feira 27/11/2026
        LocalDate friday = LocalDate.of(2026, 11, 20);
        assertThat(DeadlineCalculator.addBusinessDays(friday, 5)).isEqualTo(LocalDate.of(2026, 11, 27));
        assertThat(DeadlineCalculator.addCalendarDays(friday, 5)).isEqualTo(LocalDate.of(2026, 11, 25));
    }

    @Test
    void extractsExplicitAndDefaultDeadlinesFromPublications() {
        LocalDate pubDate = LocalDate.of(2026, 11, 16); // segunda
        var explicit = DeadlineCalculator.extractFromPublication("DESPACHO", "Despacho",
                "Intime-se a parte autora para manifestação no prazo de 10 (dez) dias úteis.", pubDate);
        assertThat(explicit).isPresent();
        assertThat(explicit.get().daysCount()).isEqualTo(10);
        assertThat(explicit.get().businessDays()).isTrue();
        assertThat(explicit.get().dueDate()).isEqualTo(LocalDate.of(2026, 11, 30));

        var calendar = DeadlineCalculator.extractFromPublication("DESPACHO", "Despacho",
                "Cumpra-se em 15 dias corridos.", pubDate);
        assertThat(calendar).isPresent();
        assertThat(calendar.get().businessDays()).isFalse();
        assertThat(calendar.get().dueDate()).isEqualTo(LocalDate.of(2026, 12, 1));

        var sentence = DeadlineCalculator.extractFromPublication("SENTENCA", "Sentença",
                "Julgo procedente o pedido.", pubDate);
        assertThat(sentence).isPresent();
        assertThat(sentence.get().daysCount()).isEqualTo(15);

        assertThat(DeadlineCalculator.extractFromPublication("OUTROS", "Outros", "Sem menção.", pubDate)).isEmpty();
    }

    @Test
    void computesUrgencyLevels() {
        LocalDate today = LocalDate.of(2026, 11, 20);
        assertThat(DeadlineCalculator.urgencyOf(DeadlineStatus.OPEN, today.minusDays(1), today))
                .isEqualTo(DeadlineUrgency.OVERDUE);
        assertThat(DeadlineCalculator.urgencyOf(DeadlineStatus.OPEN, today, today))
                .isEqualTo(DeadlineUrgency.TODAY);
        assertThat(DeadlineCalculator.urgencyOf(DeadlineStatus.OPEN, today.plusDays(2), today))
                .isEqualTo(DeadlineUrgency.SOON);
        assertThat(DeadlineCalculator.urgencyOf(DeadlineStatus.OPEN, today.plusDays(6), today))
                .isEqualTo(DeadlineUrgency.UPCOMING);
        assertThat(DeadlineCalculator.urgencyOf(DeadlineStatus.OPEN, today.plusDays(15), today))
                .isEqualTo(DeadlineUrgency.NORMAL);
        assertThat(DeadlineCalculator.urgencyOf(DeadlineStatus.COMPLETED, today.minusDays(2), today))
                .isEqualTo(DeadlineUrgency.CLOSED);
    }
}
