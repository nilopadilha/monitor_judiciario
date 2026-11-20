package br.com.monitorjudicial.djen.infrastructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Formato de transporte da API DJEN. Nenhum destes tipos sai do pacote de infraestrutura.
 *
 * <p>A API real do DJEN devolve campos duplicados com grafias distintas no mesmo objeto JSON
 * (ex.: {@code data_disponibilizacao} e {@code datadisponibilizacao}). Em records Java, usar {@code @JsonAlias}
 * causa {@code InvalidDefinitionException} no Jackson quando ambas as chaves coexistem; por isso cada chave é mapeada
 * para seu próprio componente.
 */
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
            @JsonProperty("data_disponibilizacao") String dataDisponibilizacao,
            @JsonProperty("datadisponibilizacao") String dataDisponibilizacaoAlt,
            String tipoComunicacao,
            String tipoDocumento,
            String nomeOrgao,
            String nomeClasse,
            String texto,
            @JsonProperty("numero_processo") String numeroProcesso,
            @JsonProperty("numeroProcesso") String numeroProcessoAlt,
            String link,
            List<Destinatario> destinatarios,
            List<DestinatarioAdvogado> destinatarioadvogados) {

        String resolvedDataDisponibilizacao() {
            return dataDisponibilizacao != null && !dataDisponibilizacao.isBlank()
                    ? dataDisponibilizacao
                    : dataDisponibilizacaoAlt;
        }

        String resolvedNumeroProcesso() {
            return numeroProcesso != null && !numeroProcesso.isBlank()
                    ? numeroProcesso
                    : numeroProcessoAlt;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Destinatario(String nome, String polo) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DestinatarioAdvogado(AdvogadoInfo advogado) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AdvogadoInfo(
            String nome,
            @JsonProperty("numero_oab") String numeroOab,
            @JsonProperty("uf_oab") String ufOab) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TribunalGroup(String uf, String nomeEstado, List<TribunalItem> instituicoes) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TribunalItem(String sigla, String nome) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Journal(
            String tribunal,
            String data,
            String meio,
            String url,
            @JsonProperty("total_comunicacoes") Integer totalComunicacoesSnake,
            @JsonProperty("totalComunicacoes") Integer totalComunicacoesCamel) {

        int resolvedTotalComunicacoes() {
            if (totalComunicacoesSnake != null) {
                return totalComunicacoesSnake;
            }
            return totalComunicacoesCamel == null ? 0 : totalComunicacoesCamel;
        }
    }
}
