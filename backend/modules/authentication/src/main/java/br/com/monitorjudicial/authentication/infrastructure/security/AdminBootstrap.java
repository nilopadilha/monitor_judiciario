package br.com.monitorjudicial.authentication.infrastructure.security;

import br.com.monitorjudicial.authentication.application.CreateUserCommand;
import br.com.monitorjudicial.authentication.application.UserService;
import br.com.monitorjudicial.authentication.domain.port.UserRepository;
import br.com.monitorjudicial.shared.security.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Cria o primeiro ADMIN a partir de variáveis de ambiente quando a base não tem usuários. */
@Component
class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository users;
    private final UserService userService;
    private final String email;
    private final String password;

    AdminBootstrap(UserRepository users, UserService userService,
            @Value("${security.bootstrap-admin.email:}") String email,
            @Value("${security.bootstrap-admin.password:}") String password) {
        this.users = users;
        this.userService = userService;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank() || users.count() > 0) {
            return;
        }
        userService.create(new CreateUserCommand("Administrador", email, password, Role.ADMIN, null));
        log.info("Usuário ADMIN inicial criado para {}", email);
    }
}
