package br.com.monitorjudicial.authentication.infrastructure.security;

import br.com.monitorjudicial.authentication.domain.port.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class BCryptPasswordHasher implements PasswordHasher {

    private final BCryptPasswordEncoder encoder;

    BCryptPasswordHasher(org.springframework.core.env.Environment env) {
        this.encoder = new BCryptPasswordEncoder(env.getProperty("security.bcrypt-strength", Integer.class, 12));
    }

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String hash) {
        return rawPassword != null && encoder.matches(rawPassword, hash);
    }
}
