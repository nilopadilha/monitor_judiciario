package br.com.monitorjudicial.authentication.application;

import br.com.monitorjudicial.authentication.domain.User;
import br.com.monitorjudicial.authentication.domain.port.PasswordHasher;
import br.com.monitorjudicial.authentication.domain.port.UserRepository;
import br.com.monitorjudicial.clients.api.ClientDirectory;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import br.com.monitorjudicial.shared.audit.Auditor;
import br.com.monitorjudicial.shared.domain.BusinessRuleViolationException;
import br.com.monitorjudicial.shared.domain.ConflictException;
import br.com.monitorjudicial.shared.domain.Email;
import br.com.monitorjudicial.shared.domain.ResourceNotFoundException;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    static final int MIN_PASSWORD_LENGTH = 12;

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final ClientDirectory clients;
    private final Auditor auditor;
    private final Clock clock;

    public UserService(UserRepository users, PasswordHasher passwordHasher, ClientDirectory clients, Auditor auditor,
            Clock clock) {
        this.users = users;
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

    static void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH
                || password.chars().noneMatch(Character::isDigit)
                || password.chars().noneMatch(Character::isLetter)) {
            throw new BusinessRuleViolationException("weak-password",
                    "A senha deve ter ao menos " + MIN_PASSWORD_LENGTH + " caracteres, com letras e números.");
        }
    }
}
