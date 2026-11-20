package br.com.monitorjudicial.it;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.monitorjudicial.support.IntegrationTest;
import br.com.monitorjudicial.support.TestData;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LawyersAndDiscoveryIT extends IntegrationTest {

    @Test
    void discoversProcessesForClientAndLinksLawyerByOab() throws Exception {
        String admin = adminToken();

        // 1. Cadastra advogado do escritório com OAB/SP e área Trabalhista
        JsonNode lawyer = read(mvc.perform(jsonBody(auth(post("/api/v1/lawyers"), admin), Map.of(
                        "name", "Dr. Carlos Mendes",
                        "oabNumber", "234567",
                        "oabUf", "SP",
                        "practiceArea", "TRABALHISTA",
                        "email", "carlos@escritorio.com.br",
                        "notifyEmail", true,
                        "notifyWhatsapp", false)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.oabFormatted").value("OAB/SP 234567")));
        String lawyerId = lawyer.get("id").asText();

        // 2. Cadastra cliente
        JsonNode client = createClient(admin, "Joao Pereira Descoberta", false);
        String clientId = client.get("id").asText();

        String proc1 = TestData.processNumber("26");
        String proc2 = TestData.processNumber("19");

        // 3. Simula DJEN retornando processo pelo nomeParte do cliente (mencionando OAB do advogado)
        DJEN.stubFor(WireMock.get(urlPathEqualTo("/api/v1/comunicacao"))
                .withQueryParam("nomeParte", equalTo("Joao Pereira Descoberta"))
                .willReturn(okJson("""
                        {"status":"success","count":1,"items":[{
                          "id":101,"hash":"disc-h1","siglaTribunal":"TJSP","data_disponibilizacao":"2026-11-18",
                          "tipoComunicacao":"Intimação","tipoDocumento":"Despacho","nomeOrgao":"2ª Vara",
                          "nomeClasse":"Reclamação Trabalhista",
                          "texto":"Intime-se Joao Pereira Descoberta através do advogado OAB/SP 234567.",
                          "numero_processo":"%s"
                        }]}
                        """.formatted(proc1))));

        mvc.perform(auth(post("/api/v1/clients/" + clientId + "/process-discoveries"), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processesCreated").value(1))
                .andExpect(jsonPath("$.processNumbers[0]").value(proc1));

        // Confirma que o processo foi auto-vinculado ao cliente E ao advogado casado no texto
        JsonNode processes = read(mvc.perform(auth(get("/api/v1/processes").param("lawyerId", lawyerId), admin))
                .andExpect(status().isOk()));
        assertThat(processes.get("totalElements").asLong()).isEqualTo(1);
        assertThat(processes.get("content").get(0).get("lawyerName").asText()).isEqualTo("Dr. Carlos Mendes");

        // 4. Simula DJEN retornando outro processo pela OAB do advogado (com nome do cliente no texto)
        DJEN.stubFor(WireMock.get(urlPathEqualTo("/api/v1/comunicacao"))
                .withQueryParam("numeroOab", equalTo("234567"))
                .withQueryParam("ufOab", equalTo("SP"))
                .willReturn(okJson("""
                        {"status":"success","count":1,"items":[{
                          "id":102,"hash":"disc-h2","siglaTribunal":"TJRJ","data_disponibilizacao":"2026-11-19",
                          "tipoComunicacao":"Intimação","tipoDocumento":"Sentença","nomeOrgao":"1ª Vara",
                          "nomeClasse":"Ação de Cobrança",
                          "texto":"Parte autora: Joao Pereira Descoberta. Advogado OAB/SP 234567.",
                          "numero_processo":"%s"
                        }]}
                        """.formatted(proc2))));

        mvc.perform(auth(post("/api/v1/lawyers/" + lawyerId + "/process-discoveries"), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processesCreated").value(1))
                .andExpect(jsonPath("$.processNumbers[0]").value(proc2));
    }
}
