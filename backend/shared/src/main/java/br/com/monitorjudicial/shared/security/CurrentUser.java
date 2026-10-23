package br.com.monitorjudicial.shared.security;

import java.util.Optional;
import java.util.UUID;

/** Usuário autenticado na requisição corrente, visto pelos módulos de negócio. */
public interface CurrentUser {

    UUID userId();

    String email();

    Role role();

    /** Cliente vinculado; presente apenas para o perfil CUSTOMER. */
    Optional<UUID> clientId();

    default boolean isCustomer() {
        return role() == Role.CUSTOMER;
    }

    /**
     * Cliente ao qual as consultas devem ser restritas: vazio significa sem restrição.
     * Um CUSTOMER sem vínculo é restrito a um id inexistente, nunca a "tudo".
     */
    default Optional<UUID> dataScope() {
        if (!isCustomer()) {
            return Optional.empty();
        }
        return Optional.of(clientId().orElse(new UUID(0, 0)));
    }
}
