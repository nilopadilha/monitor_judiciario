package br.com.monitorjudicial.monitoring.domain;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Classifica a evolução processual. Campos estruturados do DJEN (tipo de documento, depois tipo de comunicação) têm
 * precedência; o texto só é usado como último recurso (risco R7).
 */
public class MovementClassifier {

    private static final List<Map.Entry<String, MovementType>> RULES = List.of(
            Map.entry("sentenca", MovementType.SENTENCA),
            Map.entry("acordao", MovementType.ACORDAO),
            Map.entry("decisao", MovementType.DECISAO),
            Map.entry("despacho", MovementType.DESPACHO),
            Map.entry("citacao", MovementType.CITACAO),
            Map.entry("edital", MovementType.EDITAL),
            Map.entry("intimacao", MovementType.INTIMACAO));

    public MovementType classify(String documentType, String communicationType, String text) {
        for (String source : new String[] {documentType, communicationType}) {
            MovementType type = match(source);
            if (type != MovementType.OUTROS) {
                return type;
            }
        }
        if (text != null) {
            String head = text.length() > 200 ? text.substring(0, 200) : text;
            return match(head);
        }
        return MovementType.OUTROS;
    }

    private static MovementType match(String value) {
        if (value == null || value.isBlank()) {
            return MovementType.OUTROS;
        }
        String normalized = normalize(value);
        return RULES.stream()
                .filter(rule -> normalized.contains(rule.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(MovementType.OUTROS);
    }

    static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
