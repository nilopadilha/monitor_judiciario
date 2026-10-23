package br.com.monitorjudicial.tribunals.application;

import br.com.monitorjudicial.djen.api.DjenClient;
import br.com.monitorjudicial.djen.api.DjenTribunal;
import br.com.monitorjudicial.shared.audit.Auditor;
import br.com.monitorjudicial.shared.domain.ResourceNotFoundException;
import br.com.monitorjudicial.tribunals.api.TribunalDirectory;
import br.com.monitorjudicial.tribunals.api.TribunalSummary;
import br.com.monitorjudicial.tribunals.domain.Tribunal;
import br.com.monitorjudicial.tribunals.domain.port.TribunalRepository;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consulta e manutenção de tribunais; UC11 sincroniza com a lista de tribunais participantes do DJEN. */
@Service
public class TribunalService implements TribunalDirectory {

    public static final String CACHE = "tribunals";

    private final TribunalRepository tribunals;
    private final DjenClient djen;
    private final Auditor auditor;

    public TribunalService(TribunalRepository tribunals, DjenClient djen, Auditor auditor) {
        this.tribunals = tribunals;
        this.djen = djen;
        this.auditor = auditor;
    }

    @Transactional(readOnly = true)
    public List<TribunalView> list(Boolean active) {
        return tribunals.findAll(active).stream().map(TribunalView::of).toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CACHE, key = "#code.toUpperCase()")
    public Optional<TribunalSummary> findByCode(String code) {
        return tribunals.findByCode(code.toUpperCase(Locale.ROOT))
                .map(t -> new TribunalSummary(t.code(), t.name(), t.state(), t.active()));
    }

    @Transactional
    @CacheEvict(cacheNames = CACHE, allEntries = true)
    public TribunalView setActive(String code, boolean active) {
        Tribunal tribunal = tribunals.findByCode(code.toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("tribunal", code));
        tribunal.setActive(active);
        tribunals.save(tribunal);
        auditor.record(active ? "ACTIVATE" : "DEACTIVATE", "tribunal", tribunal.code(), Map.of());
        return TribunalView.of(tribunal);
    }

    /** Upsert dos tribunais estaduais (TJ*) informados pelo DJEN; tribunais novos entram ativos. */
    @Transactional
    @CacheEvict(cacheNames = CACHE, allEntries = true)
    public SynchronizationResult synchronize() {
        List<DjenTribunal> received = djen.getTribunals().stream()
                .filter(t -> t.code().startsWith("TJ"))
                .toList();
        int created = 0;
        int updated = 0;
        for (DjenTribunal remote : received) {
            Optional<Tribunal> existing = tribunals.findByCode(remote.code());
            if (existing.isEmpty()) {
                tribunals.save(Tribunal.register(remote.code(), remote.name(), remote.state()));
                created++;
            } else if (remote.name() != null && !remote.name().equals(existing.get().name())) {
                existing.get().rename(remote.name());
                tribunals.save(existing.get());
                updated++;
            }
        }
        auditor.record("SYNCHRONIZE", "tribunal", "*",
                Map.of("received", received.size(), "created", created, "updated", updated));
        return new SynchronizationResult(received.size(), created, updated);
    }
}
