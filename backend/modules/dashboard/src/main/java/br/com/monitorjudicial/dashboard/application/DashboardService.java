package br.com.monitorjudicial.dashboard.application;

import br.com.monitorjudicial.dashboard.application.DashboardSummary.DailyCount;
import br.com.monitorjudicial.shared.security.CurrentUser;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    static final int SERIES_DAYS = 30;

    private final DashboardReadModel readModel;
    private final Clock clock;
    private final ZoneId zone;

    public DashboardService(DashboardReadModel readModel, Clock clock,
            @Value("${monitoring.zone:America/Sao_Paulo}") ZoneId zone) {
        this.readModel = readModel;
        this.clock = clock;
        this.zone = zone;
    }

    @Transactional(readOnly = true)
    public DashboardSummary summary(CurrentUser user) {
        UUID clientId = user.dataScope().orElse(null);
        LocalDate today = LocalDate.now(clock.withZone(zone));
        LocalDate from = today.minusDays(SERIES_DAYS - 1L);
        Instant startOfDay = today.atStartOfDay(zone).toInstant();
        return new DashboardSummary(
                readModel.countProcesses(clientId, "ACTIVE"),
                readModel.countProcesses(clientId, "PAUSED"),
                readModel.countProcessesWithUnread(clientId),
                readModel.countProcessesWithFailedLastCheck(clientId),
                readModel.countUnreadPublications(clientId),
                readModel.countNotificationsSince(clientId, startOfDay, null),
                readModel.countNotificationsSince(clientId, startOfDay, "FAILED"),
                user.isCustomer() ? null : readModel.lastExecution().orElse(null),
                fillGaps(readModel.publicationsPerDay(clientId, from, today), from, today),
                readModel.publicationsByTribunal(clientId, from, today));
    }

    /** Série contínua: dias sem publicação aparecem com zero, para o gráfico não "pular" datas. */
    static List<DailyCount> fillGaps(List<DailyCount> sparse, LocalDate from, LocalDate to) {
        Map<LocalDate, Long> byDate = sparse.stream()
                .collect(Collectors.toMap(DailyCount::date, DailyCount::count, Long::sum));
        List<DailyCount> series = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            series.add(new DailyCount(d, byDate.getOrDefault(d, 0L)));
        }
        return series;
    }
}
