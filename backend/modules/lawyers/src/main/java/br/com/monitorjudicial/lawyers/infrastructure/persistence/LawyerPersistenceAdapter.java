package br.com.monitorjudicial.lawyers.infrastructure.persistence;

import br.com.monitorjudicial.lawyers.domain.Lawyer;
import br.com.monitorjudicial.lawyers.domain.OabNumber;
import br.com.monitorjudicial.lawyers.domain.PracticeArea;
import br.com.monitorjudicial.lawyers.domain.port.LawyerRepository;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import br.com.monitorjudicial.shared.domain.Email;
import br.com.monitorjudicial.shared.domain.Phone;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class LawyerPersistenceAdapter implements LawyerRepository {

    private final LawyerJpaRepository jpa;

    @Override
    public Lawyer save(Lawyer lawyer) {
        LawyerEntity e = new LawyerEntity();
        e.setId(lawyer.id());
        e.setName(lawyer.name());
        e.setOabNumber(lawyer.oab().number());
        e.setOabUf(lawyer.oab().uf());
        e.setPracticeArea(lawyer.practiceArea());
        e.setEmail(lawyer.email().value());
        e.setPhone(lawyer.phone().map(Phone::value).orElse(null));
        e.setUserId(lawyer.userId().orElse(null));
        e.setNotifyEmail(lawyer.notifyEmail());
        e.setNotifyWhatsapp(lawyer.notifyWhatsapp());
        e.setActive(lawyer.active());
        e.setCreatedAt(lawyer.createdAt());
        e.setUpdatedAt(lawyer.updatedAt());
        jpa.save(e);
        return lawyer;
    }

    @Override
    public Optional<Lawyer> findById(UUID id) {
        return jpa.findById(id).map(LawyerPersistenceAdapter::toDomain);
    }

    @Override
    public List<Lawyer> findAllById(Collection<UUID> ids) {
        return jpa.findAllById(ids).stream().map(LawyerPersistenceAdapter::toDomain).toList();
    }

    @Override
    public List<Lawyer> findByActiveTrue() {
        return jpa.findByActiveTrueOrderByNameAsc().stream().map(LawyerPersistenceAdapter::toDomain).toList();
    }

    @Override
    public boolean existsByOab(OabNumber oab) {
        return jpa.existsByOabNumberAndOabUf(oab.number(), oab.uf());
    }

    @Override
    public PageResult<Lawyer> search(String text, PracticeArea practiceArea, Boolean active, PageQuery page) {
        Specification<LawyerEntity> spec = (root, query, cb) -> cb.conjunction();
        if (text != null && !text.isBlank()) {
            String pattern = "%" + text.strip().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("oabNumber")), pattern)));
        }
        if (practiceArea != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("practiceArea"), practiceArea));
        }
        if (active != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("active"), active));
        }
        var result = jpa.findAll(spec, PageRequest.of(page.page(), page.size(), Sort.by("name")));
        return PageResult.of(result.map(LawyerPersistenceAdapter::toDomain).getContent(), page.page(), page.size(),
                result.getTotalElements());
    }

    private static Lawyer toDomain(LawyerEntity e) {
        return new Lawyer(e.getId(), e.getName(), new OabNumber(e.getOabNumber(), e.getOabUf()), e.getPracticeArea(),
                new Email(e.getEmail()), e.getPhone() == null ? null : new Phone(e.getPhone()), e.getUserId(),
                e.isNotifyEmail(), e.isNotifyWhatsapp(), e.isActive(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
