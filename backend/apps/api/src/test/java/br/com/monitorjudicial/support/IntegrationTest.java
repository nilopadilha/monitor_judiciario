package br.com.monitorjudicial.support;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Base dos testes de integração: aplicação completa + PostgreSQL, Redis e RabbitMQ reais (Testcontainers) e o DJEN
 * simulado por WireMock. O contexto é compartilhado entre as classes que não alteram propriedades.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTest {

    protected static final WireMockServer DJEN = new WireMockServer(wireMockConfig().dynamicPort());

    static {
        DJEN.start();
    }

    @DynamicPropertySource
    static void djenProperties(DynamicPropertyRegistry registry) {
        registry.add("djen.base-url", DJEN::baseUrl);
        registry.add("djen.max-attempts", () -> 1);
    }

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    @MockitoBean
    protected JavaMailSender mailSender;

    protected String adminToken() throws Exception {
        return login("admin@test.local", "admin-test-12345").get("accessToken").asText();
    }

    protected JsonNode login(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/v1/tokens").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("grantType", "PASSWORD", "email", email,
                                "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    protected MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, String token) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    protected MockHttpServletRequestBuilder jsonBody(MockHttpServletRequestBuilder request, Object body)
            throws Exception {
        return request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
    }

    protected JsonNode read(org.springframework.test.web.servlet.ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    protected JsonNode createClient(String token, String name, boolean whatsapp) throws Exception {
        var body = new java.util.HashMap<String, Object>();
        body.put("name", name);
        body.put("document", TestData.cnpj());
        body.put("email", "contato+" + System.nanoTime() + "@cliente.com.br");
        body.put("phone", "11987654321");
        body.put("notifyEmail", true);
        body.put("notifyWhatsapp", whatsapp);
        return read(mvc.perform(jsonBody(auth(post("/api/v1/clients"), token), body)).andExpect(status().isCreated()));
    }

    protected JsonNode createProcess(String token, String clientId, String number) throws Exception {
        return read(mvc.perform(jsonBody(auth(post("/api/v1/processes"), token),
                Map.of("clientId", clientId, "number", number, "subject", "Cobrança")))
                .andExpect(status().isCreated()));
    }

    protected String createCustomerUser(String adminToken, String clientId) throws Exception {
        String email = "cliente+" + System.nanoTime() + "@cliente.com.br";
        mvc.perform(jsonBody(auth(post("/api/v1/users"), adminToken), Map.of("name", "Cliente", "email", email,
                "password", "cliente-senha-123", "role", "CUSTOMER", "clientId", clientId)))
                .andExpect(status().isCreated());
        return login(email, "cliente-senha-123").get("accessToken").asText();
    }
}
