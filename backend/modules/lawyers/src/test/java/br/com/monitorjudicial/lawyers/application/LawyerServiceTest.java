package br.com.monitorjudicial.lawyers.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.monitorjudicial.lawyers.domain.Lawyer;
import br.com.monitorjudicial.lawyers.domain.OabNumber;
import br.com.monitorjudicial.lawyers.domain.PracticeArea;
import br.com.monitorjudicial.lawyers.domain.port.LawyerRepository;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import br.com.monitorjudicial.shared.audit.Auditor;
import br.com.monitorjudicial.shared.domain.ConflictException;
import br.com.monitorjudicial.shared.domain.Email;
import br.com.monitorjudicial.shared.domain.ResourceNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class LawyerServiceTest {

    private final LawyerRepository repository = mock(LawyerRepository.class);
    private final Auditor auditor = mock(Auditor.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-20T10:00:00Z"), ZoneOffset.UTC);
    private final LawyerService service = new LawyerService(repository, auditor, events, clock);

    @Test
    void createsUpdatesDeactivatesAndExposesDirectory() {
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        LawyerView created = service.create(new LawyerCommand("Dra. Ana Silva", "123.456", "sp",
                PracticeArea.TRABALHISTA, "ana@escritorio.com", "11987654321", null, true, true));

        assertThat(created.oabFormatted()).isEqualTo("OAB/SP 123456");
        assertThat(created.practiceArea()).isEqualTo(PracticeArea.TRABALHISTA);
        assertThat(created.active()).isTrue();

        Lawyer entity = Lawyer.register("Dra. Ana Silva", new OabNumber("123456", "SP"), PracticeArea.TRABALHISTA,
                new Email("ana@escritorio.com"), null, null, true, false, clock.instant());
        when(repository.findById(entity.id())).thenReturn(Optional.of(entity));
        when(repository.findAllById(Set.of(entity.id()))).thenReturn(List.of(entity));
        when(repository.findByActiveTrue()).thenReturn(List.of(entity));
        when(repository.search("ana", PracticeArea.TRABALHISTA, true, PageQuery.of(0, 20)))
                .thenReturn(PageResult.of(List.of(entity), 0, 20, 1));

        assertThat(service.update(entity.id(), new LawyerCommand("Dra. Ana Paula", "ignored", "SP",
                PracticeArea.CIVEL, "ana@escritorio.com", null, null, true, false)).name()).isEqualTo("Dra. Ana Paula");
        assertThat(service.get(entity.id()).practiceArea()).isEqualTo(PracticeArea.CIVEL);
        assertThat(service.search("ana", PracticeArea.TRABALHISTA, true, PageQuery.of(0, 20)).totalElements())
                .isEqualTo(1);
        assertThat(service.findById(entity.id())).isPresent();
        assertThat(service.findByIds(Set.of(entity.id()))).containsKey(entity.id());
        assertThat(service.findByIds(Set.of())).isEmpty();
        assertThat(service.findActive()).hasSize(1);

        service.deactivate(entity.id());
        assertThat(entity.active()).isFalse();
        verify(repository, org.mockito.Mockito.times(3)).save(any());
    }

    @Test
    void validatesOabDuplicatesAndNotFound() {
        when(repository.existsByOab(new OabNumber("99999", "RJ"))).thenReturn(true);
        assertThatThrownBy(() -> service.create(new LawyerCommand("Dr. X", "99999", "RJ",
                PracticeArea.CIVEL, "x@x.com", null, null, true, false)))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> new OabNumber("abc", "SP")).extracting("code").isEqualTo("invalid-oab");
        assertThatThrownBy(() -> new OabNumber("12345", "XX")).extracting("code").isEqualTo("invalid-oab-uf");
        assertThatThrownBy(() -> service.get(UUID.randomUUID())).isInstanceOf(ResourceNotFoundException.class);
    }
}
