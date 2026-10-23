package br.com.monitorjudicial.authentication.domain.port;

import br.com.monitorjudicial.authentication.domain.User;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import br.com.monitorjudicial.shared.domain.Email;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(Email email);

    boolean existsByEmail(Email email);

    long count();

    PageResult<User> findAll(PageQuery page);
}
