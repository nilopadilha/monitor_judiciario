package br.com.monitorjudicial.clients.infrastructure.persistence;

import br.com.monitorjudicial.clients.domain.Client;
import br.com.monitorjudicial.clients.domain.Document;
import br.com.monitorjudicial.shared.domain.Email;
import br.com.monitorjudicial.shared.domain.Phone;
import org.mapstruct.Mapper;

@Mapper
interface ClientPersistenceMapper {

    default ClientEntity toEntity(Client client) {
        ClientEntity e = new ClientEntity();
        e.setId(client.id());
        e.setName(client.name());
        e.setDocument(client.document().value());
        e.setEmail(client.email().value());
        e.setPhone(client.phone().map(Phone::value).orElse(null));
        e.setStatus(client.status());
        e.setNotifyEmail(client.notifyEmail());
        e.setNotifyWhatsapp(client.notifyWhatsapp());
        e.setCreatedAt(client.createdAt());
        e.setUpdatedAt(client.updatedAt());
        return e;
    }

    default Client toDomain(ClientEntity e) {
        return new Client(e.getId(), e.getName(), new Document(e.getDocument()), new Email(e.getEmail()),
                e.getPhone() == null ? null : new Phone(e.getPhone()), e.getStatus(), e.isNotifyEmail(),
                e.isNotifyWhatsapp(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
