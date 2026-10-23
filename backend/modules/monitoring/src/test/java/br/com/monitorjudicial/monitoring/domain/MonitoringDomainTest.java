package br.com.monitorjudicial.monitoring.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MonitoringDomainTest {

    private static final Instant NOW = Instant.parse("2026-10-23T09:00:00Z");
    private final PublicationComparisonService comparison = new PublicationComparisonService();
    private final MovementClassifier classifier = new MovementClassifier();

    static CandidatePublication candidate(String hash, String text) {
        return new CandidatePublication(hash, 1L, LocalDate.of(2026, 10, 22), "Intimação", "Sentença", "1ª Vara",
                text, "https://x", PublicationSource.DJEN);
    }

    @Test
    void newDuplicateAndRepublication() {
        PublicationHistory history = PublicationHistory.empty();
        var first = comparison.compare(candidate("h1", "Julgo procedente o pedido."), history);
        assertThat(first).isInstanceOf(ComparisonResult.New.class);

        Publication saved = Publication.fromNew((ComparisonResult.New) first, UUID.randomUUID(), UUID.randomUUID(), NOW);
        history.accept(saved);
        assertThat(history.size()).isEqualTo(1);

        assertThat(comparison.compare(candidate("h1", "outro"), history)).isInstanceOf(ComparisonResult.Duplicate.class);
        var republication = comparison.compare(candidate("h2", "  JULGO   procedente o <b>pedido.</b> "), history);
        assertThat(republication).isInstanceOfSatisfying(ComparisonResult.Republication.class,
                r -> assertThat(r.originalPublicationId()).isEqualTo(saved.id()));
    }

    @Test
    void missingHashFallsBackToFingerprint() {
        CandidatePublication noHash = candidate(" ", "texto");
        assertThat(noHash.effectiveHash()).isEqualTo(ContentFingerprint.of("texto")).hasSize(64);
    }

    @Test
    void fingerprintIgnoresCaseSpacingAndMarkup() {
        assertThat(ContentFingerprint.of("Intimação  das partes")).isEqualTo(ContentFingerprint.of("<p>INTIMAÇÃO das partes</p>"));
        assertThat(ContentFingerprint.of("o pedido.")).isEqualTo(ContentFingerprint.of("o <b>pedido</b>."));
        assertThat(ContentFingerprint.of(null)).isEqualTo(ContentFingerprint.of(""));
        assertThat(ContentFingerprint.of("a")).isNotEqualTo(ContentFingerprint.of("b"));
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
            "Sentença,Intimação,null,SENTENCA",
            "null,Citação,null,CITACAO",
            "Outros,Edital,null,EDITAL",
            "null,null,'ACÓRDÃO da 3ª Câmara',ACORDAO",
            "Certidão,Lista de distribuição,null,OUTROS",
            "null,null,null,OUTROS",
            "Despacho,null,null,DESPACHO",
            "DECISÃO,null,null,DECISAO",
            "null,Intimação,null,INTIMACAO"})
    void classifiesByStructuredFieldsFirst(String documentType, String communicationType, String text,
            MovementType expected) {
        assertThat(classifier.classify(documentType, communicationType, text)).isEqualTo(expected);
    }

    @Test
    void publicationExcerptAndReadOnce() {
        var fresh = (ComparisonResult.New) comparison.compare(candidate("h", "<p>" + "a ".repeat(400) + "</p>"),
                PublicationHistory.empty());
        Publication p = Publication.fromNew(fresh, UUID.randomUUID(), UUID.randomUUID(), NOW);
        assertThat(p.excerpt(20)).hasSize(20).endsWith("…");
        assertThat(p.readAt()).isEmpty();
        p.markRead(NOW);
        p.markRead(NOW.plusSeconds(10));
        assertThat(p.readAt()).contains(NOW);
        assertThat(p.djenId()).contains(1L);

        Movement m = Movement.fromPublication(p, MovementType.SENTENCA, NOW);
        assertThat(m.description()).startsWith("Sentença: ");
        assertThat(Movement.fromPublication(p, MovementType.OUTROS, NOW).description()).doesNotStartWith("Outros");
    }

    @Test
    void publicationRequiresCoreFields() {
        assertThatThrownBy(() -> new Publication(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), " ", "f",
                null, LocalDate.now(), null, null, null, null, null, PublicationSource.DJEN, NOW, null))
                .extracting("code").isEqualTo("invalid-publication");
    }

    @Test
    void executionLifecycle() {
        MonitoringExecution ok = MonitoringExecution.start(ExecutionTrigger.MANUAL, "a@x.com", NOW);
        assertThat(ok.status()).isEqualTo(ExecutionStatus.RUNNING);
        ok.complete(10, 3, 0, NOW.plusSeconds(5));
        assertThat(ok.status()).isEqualTo(ExecutionStatus.COMPLETED);
        assertThat(ok.finishedAt()).contains(NOW.plusSeconds(5));

        MonitoringExecution partial = MonitoringExecution.start(ExecutionTrigger.SCHEDULED, "system", NOW);
        partial.complete(10, 3, 2, NOW);
        assertThat(partial.status()).isEqualTo(ExecutionStatus.COMPLETED_WITH_FAILURES);

        MonitoringExecution failed = MonitoringExecution.start(ExecutionTrigger.SCHEDULED, "system", NOW);
        failed.fail("boom", NOW);
        assertThat(failed.errorMessage()).contains("boom");
        assertThat(failed.status()).isEqualTo(ExecutionStatus.FAILED);
    }

    @Test
    void jobFailureMessageIsTruncated() {
        MonitoringJob job = MonitoringJob.failure(UUID.randomUUID(), UUID.randomUUID(), LocalDate.now(), 10,
                "x".repeat(900), NOW);
        assertThat(job.errorMessage()).hasSize(500);
        assertThat(MonitoringJob.failure(UUID.randomUUID(), UUID.randomUUID(), LocalDate.now(), 1, null, NOW)
                .errorMessage()).isEqualTo("Erro desconhecido");
    }
}
