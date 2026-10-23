package br.com.monitorjudicial.authentication.infrastructure.persistence;

import br.com.monitorjudicial.authentication.domain.RefreshToken;
import br.com.monitorjudicial.authentication.domain.User;
import br.com.monitorjudicial.shared.domain.Email;
import org.mapstruct.Mapper;

@Mapper
interface AuthPersistenceMapper {

    default UserEntity toEntity(User user) {
        UserEntity e = new UserEntity();
        e.setId(user.id());
        e.setName(user.name());
        e.setEmail(user.email().value());
        e.setPasswordHash(user.passwordHash());
        e.setRole(user.role());
        e.setClientId(user.clientId().orElse(null));
        e.setActive(user.active());
        e.setCreatedAt(user.createdAt());
        e.setUpdatedAt(user.updatedAt());
        return e;
    }

    default User toDomain(UserEntity e) {
        return new User(e.getId(), e.getName(), new Email(e.getEmail()), e.getPasswordHash(), e.getRole(),
                e.getClientId(), e.isActive(), e.getCreatedAt(), e.getUpdatedAt());
    }

    default RefreshTokenEntity toEntity(RefreshToken token) {
        RefreshTokenEntity e = new RefreshTokenEntity();
        e.setId(token.id());
        e.setUserId(token.userId());
        e.setTokenHash(token.tokenHash());
        e.setFamilyId(token.familyId());
        e.setExpiresAt(token.expiresAt());
        e.setCreatedAt(token.createdAt());
        e.setRevokedAt(token.revokedAt());
        e.setReplacedById(token.replacedById());
        return e;
    }

    default RefreshToken toDomain(RefreshTokenEntity e) {
        return new RefreshToken(e.getId(), e.getUserId(), e.getTokenHash(), e.getFamilyId(), e.getExpiresAt(),
                e.getCreatedAt(), e.getRevokedAt(), e.getReplacedById());
    }
}
