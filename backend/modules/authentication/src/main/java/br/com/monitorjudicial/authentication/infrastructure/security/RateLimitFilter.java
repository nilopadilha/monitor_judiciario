package br.com.monitorjudicial.authentication.infrastructure.security;

import br.com.monitorjudicial.shared.security.CurrentUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limites: emissão de tokens por IP (anti força bruta) e demais rotas da API por usuário autenticado (ou IP).
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final RateLimiter limiter;
    private final ObjectMapper mapper;
    private final int tokenLimitPerMinute;
    private final int apiLimitPerMinute;

    public RateLimitFilter(RateLimiter limiter, ObjectMapper mapper, int tokenLimitPerMinute, int apiLimitPerMinute) {
        this.limiter = limiter;
        this.mapper = mapper;
        this.tokenLimitPerMinute = tokenLimitPerMinute;
        this.apiLimitPerMinute = apiLimitPerMinute;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        boolean tokenEndpoint = request.getRequestURI().startsWith("/api/v1/tokens");
        String key;
        int limit;
        if (tokenEndpoint) {
            key = "tokens:ip:" + request.getRemoteAddr();
            limit = tokenLimitPerMinute;
        } else {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            key = auth != null && auth.getPrincipal() instanceof CurrentUser user
                    ? "api:user:" + user.userId()
                    : "api:ip:" + request.getRemoteAddr();
            limit = apiLimitPerMinute;
        }
        RateLimiter.Decision decision = limiter.tryAcquire(key, limit, WINDOW);
        if (!decision.allowed()) {
            response.setHeader("Retry-After", String.valueOf(decision.retryAfterSeconds()));
            ProblemResponses.write(mapper, response, HttpStatus.TOO_MANY_REQUESTS, "rate-limited",
                    "Muitas requisições", "Limite de requisições excedido. Tente novamente em "
                            + decision.retryAfterSeconds() + " s.");
            return;
        }
        chain.doFilter(request, response);
    }
}
