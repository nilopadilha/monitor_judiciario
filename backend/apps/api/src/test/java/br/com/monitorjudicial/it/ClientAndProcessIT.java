package br.com.monitorjudicial.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.monitorjudicial.support.IntegrationTest;
import br.com.monitorjudicial.support.TestData;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ClientAndProcessIT extends IntegrationTest {

    @Test
    void clientLifecycle() throws Exception {
        String admin = adminToken();
        String document = TestData.cnpj();
        var body = new HashMap<String, Object>(Map.of("name", "Construtora Horizonte Ltda", "document", document,
                "email", "juridico@horizonte.com.br", "phone", "(11) 98765-4321", "notifyEmail", true,
                "notifyWhatsapp", true));

        JsonNode created = read(mvc.perform(jsonBody(auth(post("/api/v1/clients"), admin), body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/v1/clients/")))
                .andExpect(jsonPath("$.phone").value("+5511987654321")));
        String id = created.get("id").asText();

        mvc.perform(jsonBody(auth(post("/api/v1/clients"), admin), body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://monitorjudicial.app/problems/document-already-registered"));

        body.put("document", "11.111.111/1111-11");
        mvc.perform(jsonBody(auth(post("/api/v1/clients"), admin), body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("https://monitorjudicial.app/problems/invalid-document"));

        body.put("name", "Horizonte Engenharia");
        body.put("notifyWhatsapp", false);
        mvc.perform(jsonBody(auth(put("/api/v1/clients/" + id), admin), body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Horizonte Engenharia"));

        mvc.perform(auth(get("/api/v1/clients").param("q", TestData.digits(document).substring(0, 8)), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id));
        mvc.perform(auth(get("/api/v1/clients").param("q", "engenharia").param("status", "ACTIVE"), admin))
                .andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        mvc.perform(auth(delete("/api/v1/clients/" + id), admin)).andExpect(status().isNoContent());
        mvc.perform(auth(get("/api/v1/clients/" + id), admin)).andExpect(jsonPath("$.status").value("INACTIVE"));
        mvc.perform(auth(get("/api/v1/clients/00000000-0000-0000-0000-000000000000"), admin))
                .andExpect(status().isNotFound());
        mvc.perform(auth(get("/api/v1/clients/nao-e-uuid"), admin)).andExpect(status().isBadRequest());
    }

    @Test
    void processRegistrationRules() throws Exception {
        String admin = adminToken();
        String clientId = createClient(admin, "Processos Ltda", false).get("id").asText();
        String number = TestData.processNumber("26");

        JsonNode process = createProcess(admin, clientId, number);
        assertThat(process.get("courtCode").asText()).isEqualTo("TJSP");
        assertThat(process.get("situation").asText()).isEqualTo("MONITORED");

        mvc.perform(jsonBody(auth(post("/api/v1/processes"), admin), Map.of("clientId", clientId, "number", number)))
                .andExpect(status().isConflict());

        String wrongDigit = number.substring(0, 8) + (number.charAt(8) == '0' ? "1" : "0") + number.substring(9);
        mvc.perform(jsonBody(auth(post("/api/v1/processes"), admin), Map.of("clientId", clientId, "number", wrongDigit)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("dígito verificador")));

        String federal = "0000001-" + federalDigits() + ".2026.4.01.3400";
        mvc.perform(jsonBody(auth(post("/api/v1/processes"), admin), Map.of("clientId", clientId, "number", federal)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("https://monitorjudicial.app/problems/unsupported-court"));

        mvc.perform(jsonBody(auth(patch("/api/v1/tribunals/TJTO"), admin), Map.of("active", false)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        mvc.perform(jsonBody(auth(post("/api/v1/processes"), admin),
                        Map.of("clientId", clientId, "number", TestData.processNumber("27"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("https://monitorjudicial.app/problems/court-not-monitored"));
        mvc.perform(jsonBody(auth(patch("/api/v1/tribunals/TJTO"), admin), Map.of("active", true)))
                .andExpect(status().isOk());

        String id = process.get("id").asText();
        mvc.perform(jsonBody(auth(put("/api/v1/processes/" + id), admin), Map.of("subject", "Execução",
                "status", "PAUSED"))).andExpect(jsonPath("$.situation").value("PAUSED"));
        mvc.perform(auth(get("/api/v1/processes").param("situation", "PAUSED").param("clientId", clientId), admin))
                .andExpect(jsonPath("$.content[0].id").value(id));
        mvc.perform(auth(get("/api/v1/processes").param("number", TestData.digits(number)), admin))
                .andExpect(jsonPath("$.content[0].clientName").value("Processos Ltda"));
        mvc.perform(auth(delete("/api/v1/processes/" + id), admin)).andExpect(status().isNoContent());
        mvc.perform(jsonBody(auth(put("/api/v1/processes/" + id), admin), Map.of("status", "ACTIVE")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void customerSeesOnlyOwnProcessesAndCannotWrite() throws Exception {
        String admin = adminToken();
        String mine = createClient(admin, "Meu Cliente", false).get("id").asText();
        String other = createClient(admin, "Outro Cliente", false).get("id").asText();
        String myProcess = createProcess(admin, mine, TestData.processNumber("19")).get("id").asText();
        String otherProcess = createProcess(admin, other, TestData.processNumber("13")).get("id").asText();
        String customer = createCustomerUser(admin, mine);

        JsonNode page = read(mvc.perform(auth(get("/api/v1/processes").param("clientId", other), customer))
                .andExpect(status().isOk()));
        assertThat(page.get("content")).allSatisfy(p -> assertThat(p.get("clientId").asText()).isEqualTo(mine));
        assertThat(page.get("content").findValuesAsText("id")).contains(myProcess).doesNotContain(otherProcess);

        mvc.perform(auth(get("/api/v1/processes/" + otherProcess), customer)).andExpect(status().isNotFound());
        mvc.perform(auth(get("/api/v1/processes/" + myProcess), customer)).andExpect(status().isOk());
        mvc.perform(auth(get("/api/v1/clients"), customer))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("https://monitorjudicial.app/problems/forbidden"));
        mvc.perform(jsonBody(auth(post("/api/v1/processes"), customer),
                Map.of("clientId", mine, "number", TestData.processNumber("26")))).andExpect(status().isForbidden());
        mvc.perform(auth(post("/api/v1/monitoring-executions"), customer)).andExpect(status().isForbidden());
        mvc.perform(auth(get("/api/v1/audit-logs"), customer)).andExpect(status().isForbidden());
    }

    @Test
    void tribunalsAreSeededAndSynchronizedFromDjen() throws Exception {
        String admin = adminToken();
        mvc.perform(auth(get("/api/v1/tribunals"), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(27)));
        DJEN.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get("/api/v1/comunicacao/tribunal")
                .willReturn(com.github.tomakehurst.wiremock.client.WireMock.okJson("""
                        [{"uf":"SP","instituicoes":[{"sigla":"TJSP","nome":"Tribunal de Justiça de São Paulo"},
                                                     {"sigla":"TRT2","nome":"TRT 2ª Região"}]}]
                        """)));
        mvc.perform(auth(post("/api/v1/tribunal-synchronizations"), admin))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.received").value(1))
                .andExpect(jsonPath("$.created").value(0));
        mvc.perform(auth(get("/api/v1/tribunals").param("active", "true"), admin)).andExpect(status().isOk());
    }

    private static String federalDigits() {
        int dd = 98 - new java.math.BigInteger("0000001" + "2026" + "4" + "01" + "3400" + "00")
                .mod(java.math.BigInteger.valueOf(97)).intValue();
        return String.format("%02d", dd);
    }
}
