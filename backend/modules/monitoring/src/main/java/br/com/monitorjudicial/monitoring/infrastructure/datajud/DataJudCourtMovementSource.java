package br.com.monitorjudicial.monitoring.infrastructure.datajud;

import br.com.monitorjudicial.monitoring.domain.CandidatePublication;
import br.com.monitorjudicial.monitoring.domain.PublicationSource;
import br.com.monitorjudicial.monitoring.domain.port.CourtMovementSource;
import com.fasterxml.jackson.databind.JsonNode;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Adapter da API Pública do DataJud (CNJ) implementando {@link CourtMovementSource}.
 * Consulta metadados e andamentos públicos por número CNJ (20 dígitos) no índice {@code api_publica_<tribunal>}.
 */
@Component
@ConditionalOnProperty(name = "datajud.enabled", havingValue = "true")
@EnableConfigurationProperties(DataJudProperties.class)
class DataJudCourtMovementSource implements CourtMovementSource {

    private static final Logger log = LoggerFactory.getLogger(DataJudCourtMovementSource.class);
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");

    private final DataJudProperties properties;
    private final RestClient http;
    private final MeterRegistry meters;

    DataJudCourtMovementSource(DataJudProperties properties, RestClient.Builder builder, MeterRegistry meters) {
        this.properties = properties;
        this.http = builder.baseUrl(properties.baseUrl()).build();
        this.meters = meters;
    }

    @Override
    public List<CandidatePublication> movementsOf(String processNumber, String courtCode, LocalDate from, LocalDate to) {
        if (!properties.configured() || processNumber == null || processNumber.isBlank()
                || courtCode == null || courtCode.isBlank()) {
            return List.of();
        }
        String alias = "api_publica_" + courtCode.strip().toLowerCase(Locale.ROOT);
        Map<String, Object> query = Map.of(
                "query", Map.of("match", Map.of("numeroProcesso", processNumber.replaceAll("\\D", ""))),
                "size", 1);
        JsonNode root = executeSearch(alias, courtCode, query);
        if (root == null) {
            return List.of();
        }
        return extractCandidates(root, courtCode, from, to);
    }

    private JsonNode executeSearch(String alias, String courtCode, Map<String, Object> query) {
        try {
            JsonNode body = http.post()
                    .uri("/{alias}/_search", alias)
                    .header("Authorization", "APIKey " + properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(query)
                    .retrieve()
                    .body(JsonNode.class);
            meters.counter("datajud.requests", "court", courtCode, "outcome", "success").increment();
            return body;
        } catch (RestClientResponseException e) {
            meters.counter("datajud.requests", "court", courtCode, "outcome", String.valueOf(e.getStatusCode().value()))
                    .increment();
            log.warn("DataJud respondeu HTTP {} para {}", e.getStatusCode().value(), courtCode);
            return null;
        } catch (RestClientException e) {
            meters.counter("datajud.requests", "court", courtCode, "outcome", "unreachable").increment();
            log.warn("DataJud indisponível para {}: {}", courtCode, e.getMessage());
            return null;
        }
    }

    private List<CandidatePublication> extractCandidates(JsonNode root, String courtCode, LocalDate from, LocalDate to) {
        JsonNode hits = root.path("hits").path("hits");
        if (!hits.isArray() || hits.isEmpty()) {
            return List.of();
        }
        JsonNode source = hits.get(0).path("_source");
        String organName = textOrNull(source.path("orgaoJulgador").path("nome"));
        JsonNode movimentos = source.path("movimentos");
        if (!movimentos.isArray()) {
            return List.of();
        }
        List<CandidatePublication> result = new ArrayList<>();
        for (JsonNode mov : movimentos) {
            String rawDate = textOrNull(mov.path("dataHora"));
            LocalDate date = parseDate(rawDate);
            if (date == null || !inWindow(date, from, to)) {
                continue;
            }
            String code = mov.path("codigo").asText("");
            String name = textOrNull(mov.path("nome"));
            if (name == null) {
                continue;
            }
            String content = buildContent(name, mov.path("complementosTabelados"));
            String hash = "datajud:" + courtCode.toUpperCase(Locale.ROOT) + ":" + code + ":" + rawDate;
            result.add(new CandidatePublication(hash, null, date, "Movimentação DataJud", name, organName,
                    content, null, PublicationSource.DATAJUD));
            if (result.size() >= properties.maxHits()) {
                break;
            }
        }
        return result;
    }

    private static String buildContent(String name, JsonNode complements) {
        if (!complements.isArray() || complements.isEmpty()) {
            return name;
        }
        StringBuilder sb = new StringBuilder(name);
        for (JsonNode c : complements) {
            String desc = textOrNull(c.path("descricao"));
            String val = textOrNull(c.path("nome"));
            if (desc != null && val != null) {
                sb.append(" — ").append(desc).append(": ").append(val);
            } else if (val != null) {
                sb.append(" — ").append(val);
            }
        }
        return sb.toString();
    }

    private static boolean inWindow(LocalDate date, LocalDate from, LocalDate to) {
        boolean afterFrom = from == null || !date.isBefore(from);
        boolean beforeTo = to == null || !date.isAfter(to);
        return afterFrom && beforeTo;
    }

    static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(raw).atZoneSameInstant(BUSINESS_ZONE).toLocalDate();
        } catch (DateTimeParseException ignored) {
            try {
                return Instant.parse(raw).atZone(BUSINESS_ZONE).toLocalDate();
            } catch (DateTimeParseException ignored2) {
                return raw.length() >= 10 ? safeLocalDate(raw.substring(0, 10)) : null;
            }
        }
    }

    private static LocalDate safeLocalDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String text = node.asText();
        return text == null || text.isBlank() ? null : text.strip();
    }
}
