package br.com.monitorjudicial.lawyers.domain.port;

import br.com.monitorjudicial.lawyers.domain.Lawyer;
import br.com.monitorjudicial.lawyers.domain.OabNumber;
import br.com.monitorjudicial.lawyers.domain.PracticeArea;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LawyerRepository {

    Lawyer save(Lawyer lawyer);

    Optional<Lawyer> findById(UUID id);

    List<Lawyer> findAllById(Collection<UUID> ids);

    List<Lawyer> findByActiveTrue();

    boolean existsByOab(OabNumber oab);

    PageResult<Lawyer> search(String text, PracticeArea practiceArea, Boolean active, PageQuery page);
}
