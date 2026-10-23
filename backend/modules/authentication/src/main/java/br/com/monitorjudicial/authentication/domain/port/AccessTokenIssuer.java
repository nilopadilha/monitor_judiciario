package br.com.monitorjudicial.authentication.domain.port;

import br.com.monitorjudicial.authentication.domain.User;
import java.time.Duration;

public interface AccessTokenIssuer {

    String issue(User user);

    Duration validity();
}
