package br.com.monitorjudicial.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.monitorjudicial.support.IntegrationTest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Observabilidade, documentação e contrato OpenAPI (exportado para docs/openapi). */
@org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
class PlatformIT extends IntegrationTest {

    @Test
    void healthAndPrometheusMetrics() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        adminToken();
        String metrics = mvc.perform(get("/actuator/prometheus")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(metrics).contains("http_server_requests_seconds", "outbox_pending", "application=\"monitor-judicial\"");
    }

    @Test
    void correlationIdIsEchoed() throws Exception {
        mvc.perform(get("/actuator/health").header("X-Correlation-Id", "teste-correlacao-1"))
                .andExpect(header().string("X-Correlation-Id", "teste-correlacao-1"));
    }

    @Test
    void exportsOpenApiContractWithoutVerbsInPaths() throws Exception {
        String yaml = mvc.perform(get("/v3/api-docs.yaml")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(yaml).contains("/api/v1/monitoring-executions", "/api/v1/notifications", "/api/v1/tokens",
                "/api/v1/dashboard-summary");
        assertThat(yaml).doesNotContain("/run:", "/send:", "/login:");

        Path target = Path.of("../../../docs/openapi/openapi.yaml").normalize();
        Files.createDirectories(target.getParent());
        Files.writeString(target, yaml);
    }
}
