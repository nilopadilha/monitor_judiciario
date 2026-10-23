package br.com.monitorjudicial.djen.infrastructure;

import br.com.monitorjudicial.djen.api.DjenClient;
import br.com.monitorjudicial.djen.api.DjenCommunication;
import br.com.monitorjudicial.djen.api.DjenJournal;
import br.com.monitorjudicial.djen.api.DjenSearch;
import br.com.monitorjudicial.djen.api.DjenTribunal;
import br.com.monitorjudicial.djen.api.DjenUnavailableException;
import feign.FeignException;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(DjenProperties.class)
class DjenClientAdapter implements DjenClient {

    private static final Logger log = LoggerFactory.getLogger(DjenClientAdapter.class);

    private final DjenFeignClient feign;
    private final DjenMapper mapper;
    private final DjenProperties properties;
    private final MeterRegistry meters;

    DjenClientAdapter(DjenFeignClient feign, DjenMapper mapper, DjenProperties properties, MeterRegistry meters) {
        this.feign = feign;
        this.mapper = mapper;
        this.properties = properties;
        this.meters = meters;
    }

    @Override
    public List<DjenCommunication> searchCommunications(DjenSearch search) {
        List<DjenCommunication> result = new ArrayList<>();
        for (int page = 1; page <= properties.maxPages(); page++) {
            int current = page;
            DjenDtos.CommunicationPage response = call("search", () -> feign.searchCommunications(
                    search.processNumber(), search.courtCode(), iso(search.from()), iso(search.to()), current,
                    properties.pageSize()));
            List<DjenDtos.CommunicationItem> items = response == null ? List.of() : response.safeItems();
            items.stream().map(mapper::toCommunication).forEach(result::add);
            if (items.size() < properties.pageSize() || (response.count() > 0 && result.size() >= response.count())) {
                return result;
            }
        }
        log.warn("Busca no DJEN truncada em {} páginas para o processo {}", properties.maxPages(),
                search.processNumber());
        return result;
    }

    @Override
    public Optional<DjenCommunication> getCommunication(String hash) {
        try {
            DjenDtos.CommunicationPage page = call("communication", () -> feign.getCommunication(hash));
            return page == null ? Optional.empty() : page.safeItems().stream().findFirst().map(mapper::toCommunication);
        } catch (DjenUnavailableException e) {
            if (e.getCause() instanceof FeignException.NotFound) {
                return Optional.empty();
            }
            throw e;
        }
    }

    @Override
    public byte[] getCertificate(String hash) {
        return call("certificate", () -> feign.getCertificate(hash));
    }

    @Override
    public List<DjenTribunal> getTribunals() {
        List<DjenDtos.TribunalGroup> groups = call("tribunals", feign::getTribunals);
        if (groups == null) {
            return List.of();
        }
        return groups.stream()
                .filter(g -> g.instituicoes() != null)
                .flatMap(g -> g.instituicoes().stream()
                        .filter(i -> i.sigla() != null)
                        .map(i -> new DjenTribunal(i.sigla().strip().toUpperCase(), i.nome(), g.uf())))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(DjenTribunal::code))
                .toList();
    }

    @Override
    public DjenJournal downloadJournal(String courtCode, LocalDate date, String medium) {
        return mapper.toJournal(call("journal", () -> feign.getJournal(courtCode, iso(date), medium)));
    }

    private <T> T call(String operation, Supplier<T> request) {
        try {
            T result = request.get();
            meters.counter("djen.requests", "operation", operation, "outcome", "success").increment();
            return result;
        } catch (FeignException e) {
            boolean noResponse = e.status() < 0;
            meters.counter("djen.requests", "operation", operation, "outcome",
                    noResponse ? "unreachable" : String.valueOf(e.status())).increment();
            String reason = noResponse
                    ? "DJEN inacessível (" + rootMessage(e) + ")"
                    : "DJEN respondeu HTTP " + e.status();
            throw new DjenUnavailableException("Falha na operação DJEN '" + operation + "': " + reason, e);
        }
    }

    private static String rootMessage(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getClass().getSimpleName() + (root.getMessage() == null ? "" : ": " + root.getMessage());
    }

    private static String iso(LocalDate date) {
        return date == null ? null : date.toString();
    }
}
