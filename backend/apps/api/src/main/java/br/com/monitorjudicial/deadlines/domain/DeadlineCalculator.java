package br.com.monitorjudicial.deadlines.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cálculo e extração automática de prazos processuais (CPC/CLT).
 * Contagem em dias úteis inicia no próximo dia útil após a data da publicação e exclui sábados e domingos.
 */
public final class DeadlineCalculator {

    private static final Pattern EXPLICIT_DAYS = Pattern.compile(
            "(?:prazo(?:\\s+legal|\\s+sucessivo|\\s+comum)?\\s+de|no\\s+prazo\\s+de|em)\\s+(\\d{1,3})\\s*(?:\\([^)]{1,30}\\))?\\s*dias?(\\s+corridos|\\s+[úu]teis)?",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public record DetectedDeadline(String title, LocalDate startDate, LocalDate dueDate, int daysCount,
            boolean businessDays) {
    }

    private DeadlineCalculator() {
    }

    public static LocalDate addBusinessDays(LocalDate startDate, int days) {
        if (days <= 0) {
            return startDate;
        }
        LocalDate current = startDate;
        int added = 0;
        while (added < days) {
            current = current.plusDays(1);
            DayOfWeek dow = current.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                added++;
            }
        }
        return current;
    }

    public static LocalDate addCalendarDays(LocalDate startDate, int days) {
        return days <= 0 ? startDate : startDate.plusDays(days);
    }

    public static LocalDate computeDueDate(LocalDate startDate, int days, boolean businessDays) {
        return businessDays ? addBusinessDays(startDate, days) : addCalendarDays(startDate, days);
    }

    public static DeadlineUrgency urgencyOf(DeadlineStatus status, LocalDate dueDate, LocalDate today) {
        if (status != DeadlineStatus.OPEN) {
            return DeadlineUrgency.CLOSED;
        }
        long diff = ChronoUnit.DAYS.between(today, dueDate);
        if (diff < 0) {
            return DeadlineUrgency.OVERDUE;
        }
        if (diff == 0) {
            return DeadlineUrgency.TODAY;
        }
        if (diff <= 3) {
            return DeadlineUrgency.SOON;
        }
        if (diff <= 7) {
            return DeadlineUrgency.UPCOMING;
        }
        return DeadlineUrgency.NORMAL;
    }

    public static Optional<DetectedDeadline> extractFromPublication(String movementType, String movementLabel,
            String text, LocalDate publicationDate) {
        if (publicationDate == null) {
            return Optional.empty();
        }
        if (text != null && !text.isBlank()) {
            Matcher m = EXPLICIT_DAYS.matcher(text);
            if (m.find()) {
                int days = Integer.parseInt(m.group(1));
                if (days >= 1 && days <= 180) {
                    String qualifier = m.group(2) == null ? "" : m.group(2).toLowerCase(Locale.ROOT);
                    boolean business = !qualifier.contains("corrido");
                    String label = movementLabel == null || movementLabel.isBlank() ? "Prazo" : movementLabel;
                    String title = label + " — " + days + (business ? " dias úteis" : " dias corridos");
                    return Optional.of(new DetectedDeadline(title, publicationDate,
                            computeDueDate(publicationDate, days, business), days, business));
                }
            }
        }
        if (movementType == null) {
            return Optional.empty();
        }
        return switch (movementType.toUpperCase(Locale.ROOT)) {
            case "SENTENCA" -> Optional.of(new DetectedDeadline("Prazo recursal (Sentença — 15 dias úteis)",
                    publicationDate, addBusinessDays(publicationDate, 15), 15, true));
            case "CITACAO" -> Optional.of(new DetectedDeadline("Prazo de defesa/contestação (15 dias úteis)",
                    publicationDate, addBusinessDays(publicationDate, 15), 15, true));
            case "INTIMACAO" -> Optional.of(new DetectedDeadline("Prazo de manifestação (Intimação — 5 dias úteis)",
                    publicationDate, addBusinessDays(publicationDate, 5), 5, true));
            default -> Optional.empty();
        };
    }
}
