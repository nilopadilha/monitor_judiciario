package br.com.monitorjudicial.lawyers.application;

import br.com.monitorjudicial.lawyers.api.LawyerDirectory;
import br.com.monitorjudicial.lawyers.api.LawyerSummary;
import br.com.monitorjudicial.lawyers.domain.Lawyer;
import br.com.monitorjudicial.lawyers.domain.OabNumber;
import br.com.monitorjudicial.lawyers.domain.PracticeArea;
import br.com.monitorjudicial.lawyers.domain.port.LawyerRepository;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import br.com.monitorjudicial.shared.audit.Auditor;
import br.com.monitorjudicial.shared.domain.ConflictException;
import br.com.monitorjudicial.shared.domain.Email;
import br.com.monitorjudicial.shared.domain.Phone;
import br.com.monitorjudicial.shared.domain.ResourceNotFoundException;
import br.com.monitorjudicial.shared.discovery.LawyerRegisteredEvent;
import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cadastro e gestão dos advogados do escritório; implementa {@link LawyerDirectory}. */
@Service
public class LawyerService implements LawyerDirectory {

    private static final String RESOURCE = "lawyer";

    private final LawyerRepository lawyers;
    private final Auditor auditor;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public LawyerService(LawyerRepository lawyers, Auditor auditor, ApplicationEventPublisher events, Clock clock) {
        this.lawyers = lawyers;
        this.auditor = auditor;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public LawyerView create(LawyerCommand command) {
        OabNumber oab = new OabNumber(command.oabNumber(), command.oabUf());
        if (lawyers.existsByOab(oab)) {
            throw new ConflictException("oab-already-registered",
                    "Já existe advogado cadastrado com " + oab.formatted() + ".");
        }
        Lawyer lawyer = Lawyer.register(command.name(), oab, command.practiceArea(), new Email(command.email()),
                phoneOrNull(command.phone()), command.userId(), command.notifyEmail(), command.notifyWhatsapp(),
                clock.instant());
        lawyers.save(lawyer);
        events.publishEvent(new LawyerRegisteredEvent(lawyer.id(), lawyer.name(), oab.number(), oab.uf(),
                lawyer.practiceArea().name(), lawyer.createdAt()));
        auditor.record("CREATE", RESOURCE, lawyer.id(),
                Map.of("oab", oab.formatted(), "practiceArea", lawyer.practiceArea().name()));
        return LawyerView.of(lawyer);
    }

    @Transactional
    public LawyerView update(UUID id, LawyerCommand command) {
        Lawyer lawyer = load(id);
        lawyer.update(command.name(), command.practiceArea(), new Email(command.email()),
                phoneOrNull(command.phone()), command.userId(), command.notifyEmail(), command.notifyWhatsapp(),
                clock.instant());
        lawyers.save(lawyer);
        auditor.record("UPDATE", RESOURCE, id, Map.of("practiceArea", lawyer.practiceArea().name()));
        return LawyerView.of(lawyer);
    }

    @Transactional
    public void deactivate(UUID id) {
        Lawyer lawyer = load(id);
        lawyer.setActive(false, clock.instant());
        lawyers.save(lawyer);
        auditor.record("DEACTIVATE", RESOURCE, id, Map.of());
    }

    @Transactional(readOnly = true)
    public LawyerView get(UUID id) {
        return LawyerView.of(load(id));
    }

    @Transactional(readOnly = true)
    public PageResult<LawyerView> search(String text, PracticeArea practiceArea, Boolean active, PageQuery page) {
        return lawyers.search(text, practiceArea, active, page).map(LawyerView::of);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LawyerSummary> findById(UUID id) {
        return lawyers.findById(id).map(LawyerService::summary);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, LawyerSummary> findByIds(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return lawyers.findAllById(ids).stream()
                .map(LawyerService::summary)
                .collect(Collectors.toMap(LawyerSummary::id, Function.identity()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LawyerSummary> findActive() {
        return lawyers.findByActiveTrue().stream().map(LawyerService::summary).toList();
    }

    private Lawyer load(UUID id) {
        return lawyers.findById(id).orElseThrow(() -> new ResourceNotFoundException("advogado", id));
    }

    private static Phone phoneOrNull(String phone) {
        return phone == null || phone.isBlank() ? null : new Phone(phone);
    }

    private static LawyerSummary summary(Lawyer l) {
        return new LawyerSummary(l.id(), l.name(), l.oab().number(), l.oab().uf(), l.practiceArea().name(),
                l.email().value(), l.phone().map(Phone::value).orElse(null), l.userId().orElse(null),
                l.notifyEmail(), l.notifyWhatsapp(), l.active());
    }
}
