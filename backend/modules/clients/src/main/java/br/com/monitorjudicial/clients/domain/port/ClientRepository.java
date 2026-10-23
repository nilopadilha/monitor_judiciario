package br.com.monitorjudicial.clients.domain.port;

import br.com.monitorjudicial.clients.domain.Client;
import br.com.monitorjudicial.clients.domain.ClientStatus;
import br.com.monitorjudicial.clients.domain.Document;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientRepository {

    Client save(Client client);

    Optional<Client> findById(UUID id);

    List<Client> findAllById(Collection<UUID> ids);

    boolean existsByDocument(Document document);

    PageResult<Client> search(String text, ClientStatus status, PageQuery page);
}
