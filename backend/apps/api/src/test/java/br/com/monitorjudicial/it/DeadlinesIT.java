package br.com.monitorjudicial.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.monitorjudicial.support.IntegrationTest;
import br.com.monitorjudicial.support.TestData;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DeadlinesIT extends IntegrationTest {

    @Test
    void createsListsAlertsAndCompletesDeadlinesForLawyer() throws Exception {
        String admin = adminToken();

        JsonNode lawyer = read(mvc.perform(jsonBody(auth(post("/api/v1/lawyers"), admin), Map.of(
                        "name", "Dra. Beatriz Prazos",
                        "oabNumber", "554433",
                        "oabUf", "RJ",
                        "practiceArea", "CIVEL",
                        "email", "beatriz@escritorio.com.br",
                        "notifyEmail", true,
                        "notifyWhatsapp", false)))
                .andExpect(status().isCreated()));
        String lawyerId = lawyer.get("id").asText();

        String clientId = createClient(admin, "Cliente Prazo Ltda", false).get("id").asText();
        String procNumber = TestData.processNumber("19");
        JsonNode process = read(mvc.perform(jsonBody(auth(post("/api/v1/processes"), admin), Map.of(
                        "clientId", clientId, "lawyerId", lawyerId, "number", procNumber, "subject", "Indenização")))
                .andExpect(status().isCreated()));
        String processId = process.get("id").asText();

        // Cria prazo vencendo em 2 dias (herda advogado do processo)
        LocalDate dueDate = LocalDate.now().plusDays(2);
        JsonNode deadline = read(mvc.perform(jsonBody(auth(post("/api/v1/deadlines"), admin), Map.of(
                        "processId", processId,
                        "title", "Protocolar contestação",
                        "dueDate", dueDate.toString())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lawyerId").value(lawyerId))
                .andExpect(jsonPath("$.urgency").value("SOON")));
        String deadlineId = deadline.get("id").asText();

        mvc.perform(auth(get("/api/v1/deadlines").param("lawyerId", lawyerId).param("status", "OPEN"), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mvc.perform(auth(get("/api/v1/deadlines/summary"), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openTotal").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        // Dispara alertas de prazo vencendo para o advogado (idempotente no mesmo dia)
        JsonNode alertRes = read(mvc.perform(auth(post("/api/v1/deadlines/alerts"), admin))
                .andExpect(status().isOk()));
        assertThat(alertRes.get("alertsSent").asInt()).isGreaterThanOrEqualTo(1);
        JsonNode secondAlert = read(mvc.perform(auth(post("/api/v1/deadlines/alerts"), admin))
                .andExpect(status().isOk()));
        assertThat(secondAlert.get("alertsSent").asInt()).isEqualTo(0);

        // Marca como concluído
        mvc.perform(jsonBody(auth(patch("/api/v1/deadlines/" + deadlineId), admin), Map.of("status", "COMPLETED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.urgency").value("CLOSED"));
    }
}
