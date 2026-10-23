package br.com.monitorjudicial.tribunals.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.monitorjudicial.djen.api.DjenClient;
import br.com.monitorjudicial.djen.api.DjenTribunal;
import br.com.monitorjudicial.shared.audit.Auditor;
import br.com.monitorjudicial.shared.domain.ResourceNotFoundException;
import br.com.monitorjudicial.tribunals.domain.Tribunal;
import br.com.monitorjudicial.tribunals.domain.port.TribunalRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TribunalServiceTest {

    private final TribunalRepository repository = mock(TribunalRepository.class);
    private final DjenClient djen = mock(DjenClient.class);
    private final TribunalService service = new TribunalService(repository, djen, mock(Auditor.class));

    @Test
    void synchronizesOnlyStateCourtsCreatingAndRenaming() {
        Tribunal existing = Tribunal.register("TJSP", "Nome antigo", "SP");
        when(repository.findByCode("TJSP")).thenReturn(Optional.of(existing));
        when(repository.findByCode("TJRJ")).thenReturn(Optional.empty());
        when(djen.getTribunals()).thenReturn(List.of(
                new DjenTribunal("TJSP", "Tribunal de Justiça de São Paulo", "SP"),
                new DjenTribunal("TJRJ", "Tribunal de Justiça do Rio de Janeiro", "RJ"),
                new DjenTribunal("TRF1", "Tribunal Regional Federal da 1ª Região", "DF")));

        SynchronizationResult result = service.synchronize();

        assertThat(result).isEqualTo(new SynchronizationResult(2, 1, 1));
        assertThat(existing.name()).isEqualTo("Tribunal de Justiça de São Paulo");
        verify(repository, times(2)).save(any());
    }

    @Test
    void togglesAndLists() {
        Tribunal t = Tribunal.register("TJMG", "TJMG", "MG");
        when(repository.findByCode("TJMG")).thenReturn(Optional.of(t));
        when(repository.findAll(true)).thenReturn(List.of(t));
        assertThat(service.setActive("tjmg", false).active()).isFalse();
        assertThat(service.list(true)).hasSize(1);
        assertThat(service.findByCode("tjmg")).get().extracting("code").isEqualTo("TJMG");
        assertThatThrownBy(() -> service.setActive("TJXX", true)).isInstanceOf(ResourceNotFoundException.class);
    }
}
