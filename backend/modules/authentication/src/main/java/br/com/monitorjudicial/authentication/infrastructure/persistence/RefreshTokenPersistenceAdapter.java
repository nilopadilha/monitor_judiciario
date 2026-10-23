package br.com.monitorjudicial.authentication.infrastructure.persistence;

import br.com.monitorjudicial.authentication.domain.RefreshToken;
import br.com.monitorjudicial.authentication.domain.port.RefreshTokenRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class RefreshTokenPersistenceAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpa;
    private final AuthPersistenceMapper mapper;

    @Override
    public RefreshToken save(RefreshToken token) {
        return mapper.toDomain(jpa.save(mapper.toEntity(token)));
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    public int revokeFamily(UUID familyId, Instant now) {
        return jpa.revokeFamily(familyId, now);
    }
}
