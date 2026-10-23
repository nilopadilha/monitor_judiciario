package br.com.monitorjudicial.shared.domain;

/** Recurso inexistente ou fora do escopo do usuário (HTTP 404). */
public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + "-not-found", resource + " não encontrado: " + id);
    }
}
