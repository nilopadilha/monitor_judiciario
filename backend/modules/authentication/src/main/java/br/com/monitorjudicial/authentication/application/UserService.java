package br.com.monitorjudicial.authentication.application;

import br.com.monitorjudicial.authentication.domain.User;
import br.com.monitorjudicial.authentication.domain.port.PasswordHasher;
import br.com.monitorjudicial.authentication.domain.port.RefreshTokenRepository;
import br.com.monitorjudicial.authentication.domain.port.UserRepository;
import br.com.monitorjudicial.clients.api.ClientDirectory;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import br.com.monitorjudicial.shared.audit.Auditor;
import br.com.monitorjudicial.shared.domain.BusinessRuleViolationException;
import br.com.monitorjudicial.shared.domain.ConflictException;
import br.com.monitorjudicial.shared.domain.Email;
import br.com.monitorjudicial.shared.domain.ResourceNotFoundException;
import br.com.monitorjudicial.shared.domain.UnauthorizedException;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    static final int MIN_PASSWORD_LENGTH = 12;

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordHasher passwordHasher;
    private final ClientDirectory clients;
    private final Auditor auditor;
    private final Clock clock;

    public UserService(UserRepository users, RefreshTokenRepository refreshTokens, PasswordHasher passwordHasher,
            ClientDirectory clients, Auditor auditor, Clock clock) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordHasher = passwordHasher;
        this.clients = clients;
        this.auditor = auditor;
        this.clock = clock;
    }

    @Transactional
    public UserView create(CreateUserCommand command) {
        Email email = new Email(command.email());
        if (users.existsByEmail(email)) {
            throw new ConflictException("email-already-registered", "Já existe usuário com o e-mail " + email + ".");
        }
        validatePassword(command.password());
        if (command.clientId() != null && clients.findById(command.clientId()).isEmpty()) {
            throw new BusinessRuleViolationException("client-not-found",
                    "Cliente " + command.clientId() + " não existe.");
        }
        User user = users.save(User.create(command.name(), email, passwordHasher.hash(command.password()),
                command.role(), command.clientId(), clock.instant()));
        auditor.record("CREATE", "user", user.id(), Map.of("role", user.role().name()));
        return UserView.of(user);
    }

    @Transactional(readOnly = true)
    public UserView get(UUID id) {
        return users.findById(id).map(UserView::of).orElseThrow(() -> new ResourceNotFoundException("usuario", id));
    }

    @Transactional(readOnly = true)
    public PageResult<UserView> list(PageQuery page) {
        return users.findAll(page).map(UserView::of);
    }

    @Transactional
    public UserView setActive(UUID userId, boolean active, UUID actorId) {
        if (!active && userId.equals(actorId)) {
            throw new BusinessRuleViolationException("cannot-deactivate-self",
                    "Não é permitido desativar o próprio usuário autenticado.");
        }
        User current = users.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("usuario", userId));
        Instant now = clock.instant();
        User saved = users.save(current.withActive(active, now));
        if (!active) {
            refreshTokens.revokeAllForUser(userId, now);
        }
        auditor.record(active ? "ACTIVATE" : "DEACTIVATE", "user", userId, Map.of("active", active));
        return UserView.of(saved);
    }

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        User current = users.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("usuario", userId));
        if (!passwordHasher.matches(currentPassword, current.passwordHash())) {
            throw new UnauthorizedException("invalid-current-password", "Senha atual inválida.");
        }
        validatePassword(newPassword);
        if (passwordHasher.matches(newPassword, current.passwordHash())) {
            throw new BusinessRuleViolationException("password-unchanged",
                    "A nova senha deve ser diferente da senha atual.");
        }
        Instant now = clock.instant();
        users.save(current.withPasswordHash(passwordHasher.hash(newPassword), now));
        refreshTokens.revokeAllForUser(userId, now);
        auditor.record("CHANGE_PASSWORD", "user", userId, Map.of());
    }

    static void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH
                || password.chars().noneMatch(Character::isDigit)
                || password.chars().noneMatch(Character::isLetter)) {
            throw new BusinessRuleViolationException("weak-password",
                    "A senha deve ter ao menos " + MIN_PASSWORD_LENGTH + " caracteres, com letras e números.");
        }
    }
}
