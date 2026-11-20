package br.com.monitorjudicial.authentication.infrastructure.persistence;

import br.com.monitorjudicial.shared.lgpd.ClientAnonymizedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Anonimiza e desativa usuários CUSTOMER vinculados a um titular anonimizado (LGPD). */
@Component
@RequiredArgsConstructor
class ClientAnonymizedUserListener {

    private final UserJpaRepository users;
    private final RefreshTokenJpaRepository refreshTokens;

    @EventListener
    void onClientAnonymized(ClientAnonymizedEvent event) {
        for (UserEntity user : users.findByClientId(event.clientId())) {
            user.setName("Titular anonimizado");
            user.setEmail("anon-user-" + user.getId() + "@anonimizado.invalid");
            user.setActive(false);
            user.setUpdatedAt(event.anonymizedAt());
            users.save(user);
            refreshTokens.revokeAllForUser(user.getId(), event.anonymizedAt());
        }
    }
}
