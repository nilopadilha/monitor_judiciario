package br.com.monitorjudicial.djen.infrastructure;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Formato de transporte da API DJEN. Nenhum destes tipos sai do pacote de infraestrutura. */
final class DjenDtos {

    private DjenDtos() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CommunicationPage(String status, String message, long count, List<CommunicationItem> items) {

        List<CommunicationItem> safeItems() {
            return items == null ? List.of() : items;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CommunicationItem(
            long id,
            String hash,
            String siglaTribunal,
            @JsonProperty("data_disponibilizacao") @JsonAlias("datadisponibilizacao") String dataDisponibilizacao,
            String tipoComunicacao,
            String tipoDocumento,
            String nomeOrgao,
            String nomeClasse,
            String texto,
            @JsonProperty("numero_processo") @JsonAlias("numeroProcesso") String numeroProcesso,
            String link) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TribunalGroup(String uf, String nomeEstado, List<TribunalItem> instituicoes) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TribunalItem(String sigla, String nome) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Journal(String tribunal, String data, String meio, String url,
            @JsonAlias("total_comunicacoes") int totalComunicacoes) {
    }
}
