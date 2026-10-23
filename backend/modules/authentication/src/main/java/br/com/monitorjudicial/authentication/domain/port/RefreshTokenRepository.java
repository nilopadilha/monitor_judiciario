package br.com.monitorjudicial.authentication.domain.port;

import br.com.monitorjudicial.authentication.domain.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken token);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Revoga todos os tokens ainda ativos da família; retorna quantos foram revogados. */
    int revokeFamily(UUID familyId, Instant now);
}
