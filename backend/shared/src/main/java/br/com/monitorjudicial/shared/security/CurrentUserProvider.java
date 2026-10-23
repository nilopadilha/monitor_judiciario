package br.com.monitorjudicial.shared.security;

import java.util.Optional;

/** Porta para obter o usuário corrente; implementada pelo módulo de autenticação. */
public interface CurrentUserProvider {

    /** Usuário autenticado ou vazio em contextos de sistema (agendador, consumidores de fila). */
    Optional<CurrentUser> find();
}
