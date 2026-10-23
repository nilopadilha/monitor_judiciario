package br.com.monitorjudicial.it;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.monitorjudicial.support.IntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "security.rate-limit.tokens-per-minute=3")
class RateLimitIT extends IntegrationTest {

    @Test
    void bruteForceOnTokensIsThrottledByIpInRedis() throws Exception {
        var body = Map.of("grantType", "PASSWORD", "email", "admin@test.local", "password", "errada");
        for (int i = 0; i < 3; i++) {
            mvc.perform(jsonBody(post("/api/v1/tokens").with(r -> { r.setRemoteAddr("203.0.113.9"); return r; }), body))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(jsonBody(post("/api/v1/tokens").with(r -> { r.setRemoteAddr("203.0.113.9"); return r; }), body))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.type").value("https://monitorjudicial.app/problems/rate-limited"));
    }
}
