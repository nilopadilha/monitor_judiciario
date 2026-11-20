package br.com.monitorjudicial.lawyers.infrastructure.persistence;

import br.com.monitorjudicial.lawyers.domain.PracticeArea;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lawyers")
@Getter
@Setter
@NoArgsConstructor
public class LawyerEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "oab_number", nullable = false)
    private String oabNumber;

    @Column(name = "oab_uf", nullable = false)
    private String oabUf;

    @Enumerated(EnumType.STRING)
    @Column(name = "practice_area", nullable = false)
    private PracticeArea practiceArea;

    @Column(nullable = false)
    private String email;

    private String phone;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "notify_email", nullable = false)
    private boolean notifyEmail;

    @Column(name = "notify_whatsapp", nullable = false)
    private boolean notifyWhatsapp;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
