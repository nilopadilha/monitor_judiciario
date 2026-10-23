package br.com.monitorjudicial.monitoring.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.monitorjudicial.djen.api.DjenClient;
import br.com.monitorjudicial.djen.api.DjenCommunication;
import br.com.monitorjudicial.djen.api.DjenSearch;
import br.com.monitorjudicial.djen.api.DjenUnavailableException;
import br.com.monitorjudicial.monitoring.domain.CandidatePublication;
import br.com.monitorjudicial.monitoring.domain.port.CourtMovementSource;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ProcessCheckerTest {

    private final DjenClient djen = mock(DjenClient.class);
    private final CourtMovementSource court = mock(CourtMovementSource.class);
    private final CheckResultRecorder recorder = mock(CheckResultRecorder.class);
    private final MonitoringProperties properties = new MonitoringProperties(4, 30, 1, ZoneId.of("America/Sao_Paulo"));
    private final ProcessChecker checker = new ProcessChecker(djen, court, recorder, properties);
    private final LocalDate today = LocalDate.of(2026, 10, 23);

    @Test
    void firstCheckUsesLookbackAndFiltersOtherProcesses() {
        var process = Fixtures.process(null, Instant.parse("2026-10-20T15:00:00Z"));
        when(djen.searchCommunications(any())).thenReturn(List.of(
                communication("h1", process.number()),
                communication("h2", "99999999999999999999"),
                communication("h3", null)));
        when(recorder.recordSuccess(any(), eq(process), any(), eq(today), anyLong())).thenReturn(2);

        CheckOutcome outcome = checker.check(UUID.randomUUID(), process, today);

        assertThat(outcome).isEqualTo(CheckOutcome.success(2));
        var search = ArgumentCaptor.forClass(DjenSearch.class);
        verify(djen).searchCommunications(search.capture());
        assertThat(search.getValue().from()).isEqualTo(LocalDate.of(2026, 9, 20));
        assertThat(search.getValue().to()).isEqualTo(today);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CandidatePublication>> candidates = ArgumentCaptor.forClass(List.class);
        verify(recorder).recordSuccess(any(), eq(process), candidates.capture(), eq(today), anyLong());
        assertThat(candidates.getValue()).extracting(CandidatePublication::publicationHash).containsExactly("h1", "h3");
    }

    @Test
    void laterChecksOverlapOneDayInBusinessTimeZone() {
        // 23/10 01:30 UTC = 22/10 22:30 em São Paulo → janela começa em 21/10
        var process = Fixtures.process(Instant.parse("2026-10-23T01:30:00Z"), Instant.parse("2026-01-01T00:00:00Z"));
        CheckWindow window = CheckWindow.of(process, today, properties.zone(), 30, 1);
        assertThat(window.from()).isEqualTo(LocalDate.of(2026, 10, 21));
    }

    @Test
    void djenFailureIsRecordedAsFailedJob() {
        var process = Fixtures.process(null, Instant.parse("2026-10-20T15:00:00Z"));
        when(djen.searchCommunications(any())).thenThrow(new DjenUnavailableException("DJEN fora", null));
        UUID execution = UUID.randomUUID();

        assertThat(checker.check(execution, process, today)).isEqualTo(CheckOutcome.failure());
        verify(recorder).recordFailure(eq(execution), eq(process), eq(today), anyLong(), eq("DJEN fora"));
    }

    private static DjenCommunication communication(String hash, String number) {
        return new DjenCommunication(1, hash, "TJSP", LocalDate.of(2026, 10, 22), "Intimação", "Despacho", "Vara",
                "Classe", "Texto " + hash, number, null);
    }
}
