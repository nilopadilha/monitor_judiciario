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
    private UUID lawyerId;
    private String practiceArea;
    private final ProcessNumber number;
    private final String courtCode;
    private String subject;
    private ProcessStatus status;
    private boolean confidential;
    private Instant lastCheckedAt;
    private final Instant createdAt;
    private Instant updatedAt;

    public Process(UUID id, UUID clientId, ProcessNumber number, String subject, ProcessStatus status,
            Instant lastCheckedAt, Instant createdAt, Instant updatedAt) {
        this(id, clientId, null, null, number, subject, status, false, lastCheckedAt, createdAt, updatedAt);
    }

    public Process(UUID id, UUID clientId, ProcessNumber number, String subject, ProcessStatus status,
            boolean confidential, Instant lastCheckedAt, Instant createdAt, Instant updatedAt) {
        this(id, clientId, null, null, number, subject, status, confidential, lastCheckedAt, createdAt, updatedAt);
    }

    public Process(UUID id, UUID clientId, UUID lawyerId, String practiceArea, ProcessNumber number, String subject,
            ProcessStatus status, boolean confidential, Instant lastCheckedAt, Instant createdAt, Instant updatedAt) {
        this.id = Guard.requireNonNull(id, "invalid-process", "Id é obrigatório.");
        this.clientId = Guard.requireNonNull(clientId, "invalid-process-client", "Cliente é obrigatório.");
        this.lawyerId = lawyerId;
        this.practiceArea = normalizeArea(practiceArea);
        this.number = Guard.requireNonNull(number, "invalid-process-number", "Número é obrigatório.");
        this.courtCode = number.courtCode();
        this.status = Guard.requireNonNull(status, "invalid-process-status", "Status é obrigatório.");
        this.subject = normalizeSubject(subject);
        this.confidential = confidential;
        this.lastCheckedAt = lastCheckedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Process register(UUID clientId, ProcessNumber number, String subject, Instant now) {
        return register(clientId, null, null, number, subject, false, now);
    }

    public static Process register(UUID clientId, ProcessNumber number, String subject, boolean confidential,
            Instant now) {
        return register(clientId, null, null, number, subject, confidential, now);
    }

    public static Process register(UUID clientId, UUID lawyerId, String practiceArea, ProcessNumber number,
            String subject, boolean confidential, Instant now) {
        return new Process(Ids.newId(), clientId, lawyerId, practiceArea, number, subject, ProcessStatus.ACTIVE,
                confidential, null, now, now);
    }

    public void update(String subject, ProcessStatus status, Instant now) {
        update(subject, status, this.confidential, this.lawyerId, this.practiceArea, now);
    }

    public void update(String subject, ProcessStatus status, boolean confidential, Instant now) {
        update(subject, status, confidential, this.lawyerId, this.practiceArea, now);
    }

    public void update(String subject, ProcessStatus status, boolean confidential, UUID lawyerId, String practiceArea,
            Instant now) {
        if (this.status == ProcessStatus.ARCHIVED && status != ProcessStatus.ARCHIVED) {
            throw new BusinessRuleViolationException("process-archived",
                    "Processo arquivado não pode ser reativado; cadastre-o novamente.");
        }
        this.subject = normalizeSubject(subject);
        this.status = Guard.requireNonNull(status, "invalid-process-status", "Status é obrigatório.");
        this.confidential = confidential;
        this.lawyerId = lawyerId;
        this.practiceArea = normalizeArea(practiceArea);
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

    private static String normalizeArea(String area) {
        if (area == null || area.isBlank()) {
            return null;
        }
        String s = area.strip().toUpperCase(java.util.Locale.ROOT);
        return s.length() > 32 ? s.substring(0, 32) : s;
    }

    public UUID id() {
        return id;
    }

    public UUID clientId() {
        return clientId;
    }

    public Optional<UUID> lawyerId() {
        return Optional.ofNullable(lawyerId);
    }

    public Optional<String> practiceArea() {
        return Optional.ofNullable(practiceArea);
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

    public boolean confidential() {
        return confidential;
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
