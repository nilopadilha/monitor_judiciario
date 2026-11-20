package br.com.monitorjudicial.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.monitorjudicial.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AuthenticationIT extends IntegrationTest {

    @Test
    void loginReturnsTokenPairWithNoStoreCache() throws Exception {
        mvc.perform(jsonBody(post("/api/v1/tokens"), Map.of("grantType", "PASSWORD", "email", "admin@test.local",
                        "password", "admin-test-12345")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(900));
    }

    @Test
    void wrongPasswordIsProblemDetails401() throws Exception {
        mvc.perform(jsonBody(post("/api/v1/tokens"), Map.of("grantType", "PASSWORD", "email", "admin@test.local",
                        "password", "errada")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.type").value("https://monitorjudicial.app/problems/invalid-credentials"))
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    void refreshRotatesAndReuseRevokesTheFamily() throws Exception {
        JsonNode first = login("admin@test.local", "admin-test-12345");
        String original = first.get("refreshToken").asText();

        JsonNode second = read(mvc.perform(jsonBody(post("/api/v1/tokens"),
                Map.of("grantType", "REFRESH_TOKEN", "refreshToken", original))).andExpect(status().isOk()));
        String rotated = second.get("refreshToken").asText();
        assertThat(rotated).isNotEqualTo(original);

        mvc.perform(jsonBody(post("/api/v1/tokens"), Map.of("grantType", "REFRESH_TOKEN", "refreshToken", original)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("https://monitorjudicial.app/problems/refresh-token-reused"));
        // o token legítimo mais recente também foi revogado (família comprometida)
        mvc.perform(jsonBody(post("/api/v1/tokens"), Map.of("grantType", "REFRESH_TOKEN", "refreshToken", rotated)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesRefreshToken() throws Exception {
        JsonNode pair = login("admin@test.local", "admin-test-12345");
        String refresh = pair.get("refreshToken").asText();
        mvc.perform(jsonBody(post("/api/v1/token-revocations"), Map.of("refreshToken", refresh)))
                .andExpect(status().isNoContent());
        mvc.perform(jsonBody(post("/api/v1/tokens"), Map.of("grantType", "REFRESH_TOKEN", "refreshToken", refresh)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validationAndMissingFields() throws Exception {
        mvc.perform(jsonBody(post("/api/v1/tokens"), Map.of("grantType", "PASSWORD")))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(jsonBody(post("/api/v1/tokens"), Map.of("grantType", "REFRESH_TOKEN")))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(jsonBody(post("/api/v1/tokens"), Map.of("grantType", "MAGIC")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://monitorjudicial.app/problems/malformed-body"));
    }

    @Test
    void protectedRoutesRequireTokenAndExposeSecurityHeaders() throws Exception {
        mvc.perform(get("/api/v1/clients"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("https://monitorjudicial.app/problems/unauthenticated"));
        mvc.perform(auth(get("/api/v1/clients"), "token-invalido")).andExpect(status().isUnauthorized());
        mvc.perform(auth(get("/api/v1/users/me"), adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Content-Security-Policy", "frame-ancestors 'none'"))
                .andExpect(header().exists("X-Correlation-Id"));
    }

    @Test
    void adminManagesUsersAndCustomerCannot() throws Exception {
        String admin = adminToken();
        JsonNode client = createClient(admin, "Usuários S.A.", false);
        String customer = createCustomerUser(admin, client.get("id").asText());

        mvc.perform(auth(get("/api/v1/users"), customer)).andExpect(status().isForbidden());
        JsonNode users = read(mvc.perform(auth(get("/api/v1/users"), admin)).andExpect(status().isOk()));
        assertThat(users.get("totalElements").asLong()).isGreaterThanOrEqualTo(2);
        String userId = users.get("content").get(0).get("id").asText();
        mvc.perform(auth(get("/api/v1/users/" + userId), admin)).andExpect(status().isOk());

        mvc.perform(jsonBody(auth(post("/api/v1/users"), admin), Map.of("name", "Sem cliente", "email",
                        "sem-cliente@x.com", "password", "senha-forte-123", "role", "CUSTOMER")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("https://monitorjudicial.app/problems/customer-requires-client"));
        mvc.perform(jsonBody(auth(post("/api/v1/users"), admin), Map.of("name", "", "email", "x", "password", "1",
                        "role", "ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void refreshCookieWorksAndUserStatusAndPasswordCanBeUpdated() throws Exception {
        var loginRes = mvc.perform(jsonBody(post("/api/v1/tokens"), Map.of("grantType", "PASSWORD", "email",
                        "admin@test.local", "password", "admin-test-12345")))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(cookie().exists("mj_refresh"))
                .andReturn().getResponse();
        Cookie refreshCookie = loginRes.getCookie("mj_refresh");
        assertThat(refreshCookie).isNotNull();

        mvc.perform(jsonBody(post("/api/v1/tokens").cookie(refreshCookie), Map.of("grantType", "REFRESH_TOKEN")))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("mj_refresh"));

        String admin = adminToken();
        JsonNode created = read(mvc.perform(jsonBody(auth(post("/api/v1/users"), admin), Map.of(
                "name", "Gestor Teste", "email", "gestor-lifecycle@x.com", "password", "senha-forte-123",
                "role", "MANAGER"))).andExpect(status().isCreated()));
        String managerId = created.get("id").asText();
        JsonNode managerLogin = login("gestor-lifecycle@x.com", "senha-forte-123");
        String managerAccess = managerLogin.get("accessToken").asText();
        String managerRefresh = managerLogin.get("refreshToken").asText();

        mvc.perform(jsonBody(auth(put("/api/v1/users/me/password"), managerAccess), Map.of(
                        "currentPassword", "senha-forte-123", "newPassword", "nova-senha-forte-456")))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));

        // refresh antigo foi revogado após troca de senha
        mvc.perform(jsonBody(post("/api/v1/tokens"), Map.of("grantType", "REFRESH_TOKEN", "refreshToken", managerRefresh)))
                .andExpect(status().isUnauthorized());

        // desativação pelo admin impede novo login
        mvc.perform(jsonBody(auth(patch("/api/v1/users/" + managerId), admin), Map.of("active", false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        mvc.perform(jsonBody(post("/api/v1/tokens"), Map.of("grantType", "PASSWORD", "email", "gestor-lifecycle@x.com",
                        "password", "nova-senha-forte-456")))
                .andExpect(status().isUnauthorized());
    }
}
