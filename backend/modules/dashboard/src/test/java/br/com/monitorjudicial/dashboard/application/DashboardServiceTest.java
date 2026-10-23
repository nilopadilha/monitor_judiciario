package br.com.monitorjudicial.dashboard.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.monitorjudicial.dashboard.application.DashboardSummary.DailyCount;
import br.com.monitorjudicial.shared.security.CurrentUser;
import br.com.monitorjudicial.shared.security.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DashboardServiceTest {

    record User(UUID userId, String email, Role role, Optional<UUID> clientId) implements CurrentUser {
    }

    private final DashboardReadModel readModel = mock(DashboardReadModel.class);
    // 23/10 02:00 UTC = 22/10 23:00 em São Paulo
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-23T02:00:00Z"), ZoneOffset.UTC);
    private final DashboardService service = new DashboardService(readModel, clock, ZoneId.of("America/Sao_Paulo"));

    @Test
    void usesBusinessDayAndFillsSeries() {
        when(readModel.countProcesses(null, "ACTIVE")).thenReturn(5L);
        when(readModel.publicationsPerDay(any(), any(), any()))
                .thenReturn(List.of(new DailyCount(LocalDate.of(2026, 10, 20), 3)));
        when(readModel.lastExecution()).thenReturn(Optional.empty());

        DashboardSummary summary = service.summary(new User(UUID.randomUUID(), "m", Role.MANAGER, Optional.empty()));

        assertThat(summary.monitoredProcesses()).isEqualTo(5);
        assertThat(summary.publicationsPerDay()).hasSize(DashboardService.SERIES_DAYS);
        assertThat(summary.publicationsPerDay().getLast().date()).isEqualTo(LocalDate.of(2026, 10, 22));
        assertThat(summary.publicationsPerDay()).filteredOn(d -> d.count() > 0).singleElement()
                .extracting(DailyCount::date).isEqualTo(LocalDate.of(2026, 10, 20));
        verify(readModel).countNotificationsSince(isNull(), eq(Instant.parse("2026-10-22T03:00:00Z")), isNull());
    }

    @Test
    void customerIsScopedAndDoesNotSeeExecutions() {
        UUID client = UUID.randomUUID();
        DashboardSummary summary = service.summary(new User(UUID.randomUUID(), "c", Role.CUSTOMER, Optional.of(client)));
        assertThat(summary.lastExecution()).isNull();
        verify(readModel).countProcesses(client, "ACTIVE");
        verify(readModel).countUnreadPublications(client);
    }
}
