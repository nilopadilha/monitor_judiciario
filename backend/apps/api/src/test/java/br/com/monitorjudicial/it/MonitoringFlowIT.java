package br.com.monitorjudicial.it;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.monitorjudicial.support.IntegrationTest;
import br.com.monitorjudicial.support.TestData;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;

/**
 * Fluxo completo do PDF: consultar DJEN → detectar → comparar → persistir evento → outbox → RabbitMQ → notificar →
 * histórico → painel.
 */
class MonitoringFlowIT extends IntegrationTest {

    @Test
    void endToEndMonitoringFlow() throws Exception {
        String admin = adminToken();
        JsonNode client = createClient(admin, "Fluxo Completo Ltda", true);
        String clientId = client.get("id").asText();
        String number = TestData.processNumber("26");
        String digits = TestData.digits(number);
        String processId = createProcess(admin, clientId, number).get("id").asText();
        String failingNumber = TestData.processNumber("21");
        String failingId = createProcess(admin, clientId, failingNumber).get("id").asText();
        String customer = createCustomerUser(admin, clientId);
        LocalDate today = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

        DJEN.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlPathEqualTo("/api/v1/comunicacao"))
                .withQueryParam("numeroProcesso", equalTo(digits))
                .willReturn(okJson(page(
                        item(101, "hash-sentenca", "Sentença", "Julgo parcialmente procedente o pedido.", number, today),
                        item(102, "hash-despacho", "Despacho", "Intimem-se as partes para especificar provas.", number,
                                today.minusDays(1)),
                        item(103, "hash-retificacao", "Sentença", "JULGO  parcialmente procedente o <b>pedido</b>.",
                                number, today)))));
        DJEN.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlPathEqualTo("/api/v1/comunicacao"))
                .withQueryParam("numeroProcesso", equalTo(TestData.digits(failingNumber)))
                .willReturn(aResponse().withStatus(503)));

        String location = mvc.perform(auth(post("/api/v1/monitoring-executions"), admin))
                .andExpect(status().isAccepted())
                .andExpect(header().exists("Location"))
                .andReturn().getResponse().getHeader("Location");

        JsonNode execution = awaitExecution(admin, location);
        assertThat(execution.get("status").asText()).isEqualTo("COMPLETED_WITH_FAILURES");
        assertThat(execution.get("failedJobs").asInt()).isGreaterThanOrEqualTo(1);
        JsonNode jobs = execution.get("jobs");
        assertThat(jobs.findValuesAsText("processNumber")).contains(number, failingNumber);

        JsonNode publications = read(mvc.perform(auth(get("/api/v1/publications").param("processId", processId), admin))
                .andExpect(status().isOk()));
        assertThat(publications.get("totalElements").asInt()).as("retificação não vira novidade").isEqualTo(2);

        JsonNode movements = read(mvc.perform(auth(get("/api/v1/processes/" + processId + "/movements"), admin)));
        assertThat(movements.get("content").findValuesAsText("type")).containsExactly("SENTENCA", "DESPACHO");

        mvc.perform(auth(get("/api/v1/processes/" + processId), admin))
                .andExpect(jsonPath("$.situation").value("NEW_PUBLICATION"))
                .andExpect(jsonPath("$.unreadPublications").value(2));
        mvc.perform(auth(get("/api/v1/processes/" + failingId), admin))
                .andExpect(jsonPath("$.situation").value("FAILED"))
                .andExpect(jsonPath("$.lastError").value(org.hamcrest.Matchers.containsString("503")));

        // outbox → RabbitMQ → notificações (e-mail entregue; WhatsApp sem credenciais falha com motivo)
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            JsonNode notifications = read(mvc.perform(auth(get("/api/v1/notifications")
                    .param("processId", processId), admin)));
            assertThat(notifications.get("totalElements").asInt()).isEqualTo(4);
        });
        verify(mailSender, atLeast(2)).send(any(SimpleMailMessage.class));
        mvc.perform(auth(get("/api/v1/notifications").param("processId", processId).param("channel", "WHATSAPP")
                        .param("status", "FAILED"), admin))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].errorMessage").value(org.hamcrest.Matchers.containsString(
                        "não configurado")));

        // painel do cliente
        mvc.perform(auth(get("/api/v1/dashboard-summary"), customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monitoredProcesses").value(2))
                .andExpect(jsonPath("$.processesWithNews").value(1))
                .andExpect(jsonPath("$.processesWithFailedCheck").value(1))
                .andExpect(jsonPath("$.unreadPublications").value(2))
                .andExpect(jsonPath("$.notificationsToday").value(4))
                .andExpect(jsonPath("$.failedNotificationsToday").value(2))
                .andExpect(jsonPath("$.lastExecution").doesNotExist())
                .andExpect(jsonPath("$.publicationsPerDay.length()").value(30))
                .andExpect(jsonPath("$.publicationsByTribunal[0].courtCode").value("TJSP"));
        mvc.perform(auth(get("/api/v1/dashboard-summary"), admin))
                .andExpect(jsonPath("$.lastExecution.status").exists());

        // leitura
        String publicationId = publications.get("content").get(0).get("id").asText();
        mvc.perform(jsonBody(auth(patch("/api/v1/publications/" + publicationId), customer), Map.of("read", true)))
                .andExpect(status().isForbidden());
        mvc.perform(jsonBody(auth(patch("/api/v1/publications/" + publicationId), admin), Map.of("read", true)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.read").value(true));
        mvc.perform(jsonBody(auth(patch("/api/v1/publications/" + publicationId), admin), Map.of("read", false)))
                .andExpect(status().isBadRequest());
        mvc.perform(auth(get("/api/v1/publications").param("unread", "true").param("processId", processId), customer))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(auth(get("/api/v1/publications/" + publicationId), customer)).andExpect(status().isOk());

        // reexecução: idempotente, nenhuma publicação ou notificação duplicada
        String second = mvc.perform(auth(post("/api/v1/monitoring-executions"), admin))
                .andExpect(status().isAccepted()).andReturn().getResponse().getHeader("Location");
        awaitExecution(admin, second);
        mvc.perform(auth(get("/api/v1/publications").param("processId", processId), admin))
                .andExpect(jsonPath("$.totalElements").value(2));
        Thread.sleep(1000);
        mvc.perform(auth(get("/api/v1/notifications").param("processId", processId), admin))
                .andExpect(jsonPath("$.totalElements").value(4));

        // envio manual e histórico
        mvc.perform(jsonBody(auth(post("/api/v1/notifications"), admin), Map.of("processId", processId,
                        "channel", "EMAIL", "message", "Audiência redesignada para 12/11.")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SENT"));
        mvc.perform(auth(get("/api/v1/monitoring-executions"), admin))
                .andExpect(jsonPath("$.content[0].trigger").value("MANUAL"));
        mvc.perform(auth(get("/api/v1/audit-logs").param("resourceType", "monitoring-execution"), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].actorEmail").value("admin@test.local"));
        mvc.perform(auth(get("/api/v1/audit-logs").param("resourceType", "publication")
                        .param("resourceId", publicationId), admin))
                .andExpect(jsonPath("$.content[0].action").value("MARK_READ"));
    }

    private JsonNode awaitExecution(String token, String location) {
        return await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(200)).until(
                () -> read(mvc.perform(auth(get(location), token))),
                node -> !"RUNNING".equals(node.get("status").asText()));
    }

    private static String page(String... items) {
        return "{\"status\":\"success\",\"count\":" + items.length + ",\"items\":[" + String.join(",", items) + "]}";
    }

    private static String item(long id, String hash, String documentType, String text, String number, LocalDate date) {
        return """
                {"id":%d,"hash":"%s","siglaTribunal":"TJSP","data_disponibilizacao":"%s","tipoComunicacao":"Intimação",
                 "tipoDocumento":"%s","nomeOrgao":"2ª Vara Cível","texto":"%s","numero_processo":"%s"}
                """.formatted(id, hash, date, documentType, text, number);
    }
}
