package br.com.monitorjudicial.djen.api;

import java.time.LocalDate;

/**
 * Comunicação processual publicada no DJEN, já traduzida para o vocabulário do sistema (Anti-Corruption Layer).
 *
 * @param processNumber número CNJ somente com dígitos
 */
public record DjenCommunication(
        long djenId,
        String hash,
        String courtCode,
        LocalDate availableAt,
        String communicationType,
        String documentType,
        String organName,
        String className,
        String text,
        String processNumber,
        String link) {
}
