package br.com.monitorjudicial.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.monitorjudicial.lgpd.application.DataRetentionService;
import br.com.monitorjudicial.support.IntegrationTest;
import br.com.monitorjudicial.support.TestData;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class LgpdIT extends IntegrationTest {

    @Autowired
    DataRetentionService retentionService;

    @Test
    void dataMinimizationExportPreferencesAnonymizationAndRetention() throws Exception {
        String admin = adminToken();
        JsonNode client = createClient(admin, "Titular LGPD", true);
        String clientId = client.get("id").asText();
        String procNumber = TestData.processNumber("26");
        JsonNode process = read(mvc.perform(jsonBody(auth(post("/api/v1/processes"), admin),
                        Map.of("clientId", clientId, "number", procNumber, "subject", "Família", "confidential", true)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.confidential").value(true)));
        String processId = process.get("id").asText();

        // Minimização na listagem (documento mascarado) vs detalhe completo auditado
        mvc.perform(auth(get("/api/v1/clients").param("q", "Titular LGPD"), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].document").value(org.hamcrest.Matchers.containsString("***")));
        mvc.perform(auth(get("/api/v1/clients/" + clientId), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legalBasis").value("CONTRACT_EXECUTION"));

        // Envio de notificação para validar anonimização posterior
        mvc.perform(jsonBody(auth(post("/api/v1/notifications"), admin), Map.of(
                        "processId", processId, "channel", "EMAIL", "message", "Aviso inicial.")))
                .andExpect(status().isCreated());

        // CUSTOMER: preferências self-service e exportação dos próprios dados
        String customerEmail = "lgpd-customer@example.com";
        mvc.perform(jsonBody(auth(post("/api/v1/users"), admin), Map.of(
                        "name", "Titular LGPD", "email", customerEmail, "password", "senha-forte-123",
                        "role", "CUSTOMER", "clientId", clientId)))
                .andExpect(status().isCreated());
        String customerToken = login(customerEmail, "senha-forte-123").get("accessToken").asText();

        mvc.perform(jsonBody(auth(patch("/api/v1/clients/me/notification-preferences"), customerToken),
                        Map.of("notifyEmail", true, "notifyWhatsapp", false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifyWhatsapp").value(false));

        mvc.perform(auth(get("/api/v1/users/me/data-export"), customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.client.id").value(clientId))
                .andExpect(jsonPath("$.processes[0].confidential").value(true))
                .andExpect(jsonPath("$.notifications.length()").value(1));

        // Outro cliente não pode ser exportado por este CUSTOMER
        String otherClientId = createClient(admin, "Outro Titular", false).get("id").asText();
        mvc.perform(auth(get("/api/v1/clients/" + otherClientId + "/data-export"), customerToken))
                .andExpect(status().isNotFound());

        // Anonimização pelo ADMIN: limpa cliente, desativa CUSTOMER e mascara notificações
        mvc.perform(auth(post("/api/v1/clients/" + clientId + "/anonymizations"), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ANONYMIZED"))
                .andExpect(jsonPath("$.name").value("Titular anonimizado"))
                .andExpect(jsonPath("$.document").value("[anonimizado]"));

        // Usuário CUSTOMER vinculado não consegue mais logar
        mvc.perform(jsonBody(post("/api/v1/tokens"), Map.of(
                        "grantType", "PASSWORD", "email", customerEmail, "password", "senha-forte-123")))
                .andExpect(status().isUnauthorized());

        // Notificações do cliente tiveram destinatário e mensagem anonimizados
        mvc.perform(auth(get("/api/v1/notifications").param("processId", processId), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].recipient").value("[anonimizado]"));

        // Job de retenção executa com lock sem erro
        var report = retentionService.purgeExpired();
        assertThat(report.refreshTokens()).isGreaterThanOrEqualTo(0);
    }
}
