package br.com.monitorjudicial.processes.domain;

import br.com.monitorjudicial.shared.domain.BusinessRuleViolationException;
import br.com.monitorjudicial.shared.domain.Guard;
import br.com.monitorjudicial.shared.domain.Ids;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Processo judicial monitorado. O tribunal é sempre derivado do número CNJ. */
public final class Process {

    private final UUID id;
    private final UUID clientId;
    private final ProcessNumber number;
    private final String courtCode;
    private String subject;
    private ProcessStatus status;
    private Instant lastCheckedAt;
    private final Instant createdAt;
    private Instant updatedAt;

    public Process(UUID id, UUID clientId, ProcessNumber number, String subject, ProcessStatus status,
            Instant lastCheckedAt, Instant createdAt, Instant updatedAt) {
        this.id = Guard.requireNonNull(id, "invalid-process", "Id é obrigatório.");
        this.clientId = Guard.requireNonNull(clientId, "invalid-process-client", "Cliente é obrigatório.");
        this.number = Guard.requireNonNull(number, "invalid-process-number", "Número é obrigatório.");
        this.courtCode = number.courtCode();
        this.status = Guard.requireNonNull(status, "invalid-process-status", "Status é obrigatório.");
        this.subject = normalizeSubject(subject);
        this.lastCheckedAt = lastCheckedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Process register(UUID clientId, ProcessNumber number, String subject, Instant now) {
        return new Process(Ids.newId(), clientId, number, subject, ProcessStatus.ACTIVE, null, now, now);
    }

    public void update(String subject, ProcessStatus status, Instant now) {
        if (this.status == ProcessStatus.ARCHIVED && status != ProcessStatus.ARCHIVED) {
            throw new BusinessRuleViolationException("process-archived",
                    "Processo arquivado não pode ser reativado; cadastre-o novamente.");
        }
        this.subject = normalizeSubject(subject);
        this.status = Guard.requireNonNull(status, "invalid-process-status", "Status é obrigatório.");
        this.updatedAt = now;
    }

    public void archive(Instant now) {
        this.status = ProcessStatus.ARCHIVED;
        this.updatedAt = now;
    }

    public void markChecked(Instant at) {
        this.lastCheckedAt = at;
    }

    public boolean isMonitorable() {
        return status == ProcessStatus.ACTIVE;
    }

    private static String normalizeSubject(String subject) {
        if (subject == null || subject.isBlank()) {
            return null;
        }
        String s = subject.strip();
        if (s.length() > 300) {
            throw new BusinessRuleViolationException("invalid-subject", "Assunto deve ter até 300 caracteres.");
        }
        return s;
    }

    public UUID id() {
        return id;
    }

    public UUID clientId() {
        return clientId;
    }

    public ProcessNumber number() {
        return number;
    }

    public String courtCode() {
        return courtCode;
    }

    public Optional<String> subject() {
        return Optional.ofNullable(subject);
    }

    public ProcessStatus status() {
        return status;
    }

    public Optional<Instant> lastCheckedAt() {
        return Optional.ofNullable(lastCheckedAt);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
