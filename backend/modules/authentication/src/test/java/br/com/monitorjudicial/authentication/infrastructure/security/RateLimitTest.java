package br.com.monitorjudicial.authentication.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-23T10:00:30Z"), ZoneOffset.UTC);

    @Test
    @SuppressWarnings("unchecked")
    void redisLimiterBlocksAfterLimitAndFailsOpen() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.increment(anyString())).thenReturn(1L, 11L);
        var limiter = new RedisRateLimiter(redis, clock);

        assertThat(limiter.tryAcquire("k", 10, Duration.ofMinutes(1)).allowed()).isTrue();
        verify(redis).expire(anyString(), eq(Duration.ofSeconds(61)));
        var blocked = limiter.tryAcquire("k", 10, Duration.ofMinutes(1));
        assertThat(blocked.allowed()).isFalse();
        assertThat(blocked.retryAfterSeconds()).isEqualTo(30);

        when(ops.increment(anyString())).thenThrow(new RedisConnectionFailureException("down"));
        assertThat(limiter.tryAcquire("k", 10, Duration.ofMinutes(1)).allowed()).isTrue();
    }

    @Test
    void filterReturns429WithRetryAfterOnTokenEndpoint() throws Exception {
        RateLimiter limiter = mock(RateLimiter.class);
        when(limiter.tryAcquire(eq("tokens:ip:10.0.0.1"), eq(10), any()))
                .thenReturn(new RateLimiter.Decision(false, 42));
        var filter = new RateLimitFilter(limiter, new ObjectMapper(), 10, 300);
        var request = new MockHttpServletRequest("POST", "/api/v1/tokens");
        request.setRemoteAddr("10.0.0.1");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { throw new AssertionError("não deveria seguir"); });

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("42");
        assertThat(response.getContentAsString()).contains("rate-limited");
    }

    @Test
    void filterSkipsNonApiAndAllowsUnderLimit() throws Exception {
        RateLimiter limiter = mock(RateLimiter.class);
        when(limiter.tryAcquire(anyString(), eq(300), any())).thenReturn(new RateLimiter.Decision(true, 0));
        var filter = new RateLimitFilter(limiter, new ObjectMapper(), 10, 300);
        var passed = new boolean[2];

        filter.doFilter(new MockHttpServletRequest("GET", "/actuator/health"), new MockHttpServletResponse(),
                (req, res) -> passed[0] = true);
        filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/clients"), new MockHttpServletResponse(),
                (req, res) -> passed[1] = true);

        assertThat(passed).containsExactly(true, true);
    }
}
