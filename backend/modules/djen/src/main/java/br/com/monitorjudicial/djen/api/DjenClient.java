package br.com.monitorjudicial.djen.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Porta de integração com a API pública do DJEN, conforme o PDF:
 * searchCommunications, getCommunication, getCertificate, getTribunals, downloadJournal.
 */
public interface DjenClient {

    /** Todas as comunicações que atendem ao filtro (a paginação do DJEN é percorrida internamente). */
    List<DjenCommunication> searchCommunications(DjenSearch search);

    Optional<DjenCommunication> getCommunication(String hash);

    /** Certidão de publicação em PDF. */
    byte[] getCertificate(String hash);

    List<DjenTribunal> getTribunals();

    DjenJournal downloadJournal(String courtCode, LocalDate date, String medium);
}
