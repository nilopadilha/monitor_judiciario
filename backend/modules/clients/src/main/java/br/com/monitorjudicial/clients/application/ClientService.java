package br.com.monitorjudicial.clients.application;

import br.com.monitorjudicial.clients.api.ClientDirectory;
import br.com.monitorjudicial.clients.api.ClientSummary;
import br.com.monitorjudicial.clients.domain.Client;
import br.com.monitorjudicial.clients.domain.ClientStatus;
import br.com.monitorjudicial.clients.domain.Document;
import br.com.monitorjudicial.clients.domain.port.ClientRepository;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import br.com.monitorjudicial.shared.audit.Auditor;
import br.com.monitorjudicial.shared.domain.ConflictException;
import br.com.monitorjudicial.shared.domain.Email;
import br.com.monitorjudicial.shared.domain.Phone;
import br.com.monitorjudicial.shared.domain.ResourceNotFoundException;
import java.time.Clock;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** UC03 — cadastro de clientes; também implementa o contrato público {@link ClientDirectory}. */
@Service
public class ClientService implements ClientDirectory {

    private static final String RESOURCE = "client";

    private final ClientRepository clients;
    private final Auditor auditor;
    private final Clock clock;

    public ClientService(ClientRepository clients, Auditor auditor, Clock clock) {
        this.clients = clients;
        this.auditor = auditor;
        this.clock = clock;
    }

    @Transactional
    public ClientView create(ClientCommand command) {
        Document document = new Document(command.document());
        if (clients.existsByDocument(document)) {
            throw new ConflictException("document-already-registered",
                    "Já existe cliente com o documento " + document.formatted() + ".");
        }
        Client client = Client.register(command.name(), document, new Email(command.email()),
                phoneOrNull(command.phone()), command.notifyEmail(), command.notifyWhatsapp(), clock.instant());
        clients.save(client);
        auditor.record("CREATE", RESOURCE, client.id(), Map.of("document", document.masked()));
        return ClientView.of(client);
    }

    @Transactional
    public ClientView update(UUID id, ClientCommand command) {
        Client client = load(id);
        client.update(command.name(), new Email(command.email()), phoneOrNull(command.phone()),
                command.notifyEmail(), command.notifyWhatsapp(), clock.instant());
        clients.save(client);
        auditor.record("UPDATE", RESOURCE, id, Map.of());
        return ClientView.of(client);
    }

    @Transactional
    public void deactivate(UUID id) {
        Client client = load(id);
        client.deactivate(clock.instant());
        clients.save(client);
        auditor.record("DEACTIVATE", RESOURCE, id, Map.of());
    }

    @Transactional(readOnly = true)
    public ClientView get(UUID id) {
        return ClientView.of(load(id));
    }

    @Transactional(readOnly = true)
    public PageResult<ClientView> search(String text, ClientStatus status, PageQuery page) {
        return clients.search(text, status, page).map(ClientView::of);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClientSummary> findById(UUID id) {
        return clients.findById(id).map(ClientService::summary);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, ClientSummary> findByIds(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return clients.findAllById(ids).stream()
                .map(ClientService::summary)
                .collect(Collectors.toMap(ClientSummary::id, Function.identity()));
    }

    private Client load(UUID id) {
        return clients.findById(id).orElseThrow(() -> new ResourceNotFoundException("cliente", id));
    }

    private static Phone phoneOrNull(String phone) {
        return phone == null || phone.isBlank() ? null : new Phone(phone);
    }

    private static ClientSummary summary(Client c) {
        return new ClientSummary(c.id(), c.name(), c.email().value(), c.phone().map(Phone::value).orElse(null),
                c.notifyEmail(), c.notifyWhatsapp(), c.isActive());
    }
}
