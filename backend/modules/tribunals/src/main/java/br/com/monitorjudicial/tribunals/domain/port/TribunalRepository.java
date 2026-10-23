package br.com.monitorjudicial.tribunals.domain.port;

import br.com.monitorjudicial.tribunals.domain.Tribunal;
import java.util.List;
import java.util.Optional;

public interface TribunalRepository {

    Tribunal save(Tribunal tribunal);

    Optional<Tribunal> findByCode(String code);

    List<Tribunal> findAll(Boolean active);
}
