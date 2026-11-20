package br.com.monitorjudicial.clients.infrastructure.persistence;

import br.com.monitorjudicial.clients.domain.Client;
import br.com.monitorjudicial.clients.domain.ClientStatus;
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
        e.setDocument(client.storedDocument());
        e.setEmail(client.email().value());
        e.setPhone(client.phone().map(Phone::value).orElse(null));
        e.setStatus(client.status());
        e.setNotifyEmail(client.notifyEmail());
        e.setNotifyWhatsapp(client.notifyWhatsapp());
        e.setLegalBasis(client.legalBasis());
        e.setPrivacyNoticeVersion(client.privacyNoticeVersion());
        e.setConsentRecordedAt(client.consentRecordedAt());
        e.setAnonymizedAt(client.anonymizedAt());
        e.setCreatedAt(client.createdAt());
        e.setUpdatedAt(client.updatedAt());
        return e;
    }

    default Client toDomain(ClientEntity e) {
        Document doc = e.getStatus() == ClientStatus.ANONYMIZED ? null : new Document(e.getDocument());
        return new Client(e.getId(), e.getName(), doc, e.getDocument(), new Email(e.getEmail()),
                e.getPhone() == null ? null : new Phone(e.getPhone()), e.getStatus(), e.isNotifyEmail(),
                e.isNotifyWhatsapp(), e.getLegalBasis(), e.getPrivacyNoticeVersion(), e.getConsentRecordedAt(),
                e.getAnonymizedAt(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
