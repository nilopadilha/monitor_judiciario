package br.com.monitorjudicial.djen.api;

import java.time.LocalDate;
import java.util.List;

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
        String link,
        List<String> recipientNames,
        List<String> lawyerOabs) {

    public DjenCommunication {
        recipientNames = recipientNames == null ? List.of() : List.copyOf(recipientNames);
        lawyerOabs = lawyerOabs == null ? List.of() : List.copyOf(lawyerOabs);
    }

    public DjenCommunication(long djenId, String hash, String courtCode, LocalDate availableAt,
            String communicationType, String documentType, String organName, String className,
            String text, String processNumber, String link) {
        this(djenId, hash, courtCode, availableAt, communicationType, documentType, organName, className,
                text, processNumber, link, List.of(), List.of());
    }
}
