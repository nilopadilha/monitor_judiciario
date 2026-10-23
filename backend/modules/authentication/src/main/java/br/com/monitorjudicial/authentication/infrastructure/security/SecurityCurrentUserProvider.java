package br.com.monitorjudicial.authentication.infrastructure.security;

import br.com.monitorjudicial.shared.security.CurrentUser;
import br.com.monitorjudicial.shared.security.CurrentUserProvider;
import java.util.Optional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
class SecurityCurrentUserProvider implements CurrentUserProvider {

    @Override
    public Optional<CurrentUser> find() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CurrentUser user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }
}
