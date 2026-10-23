package br.com.monitorjudicial.authentication.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Cadeia de segurança stateless. CSRF desabilitado porque não há cookies de sessão: o token viaja no cabeçalho
 * Authorization.
 */
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtTokenService tokens, RateLimiter rateLimiter,
            ObjectMapper mapper,
            @Value("${security.rate-limit.tokens-per-minute:10}") int tokensPerMinute,
            @Value("${security.rate-limit.api-per-minute:300}") int apiPerMinute) throws Exception {
        var jwtFilter = new JwtAuthenticationFilter(tokens);
        var rateLimitFilter = new RateLimitFilter(rateLimiter, mapper, tokensPerMinute, apiPerMinute);
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(h -> h
                        .contentSecurityPolicy(csp -> csp.policyDirectives("frame-ancestors 'none'"))
                        .frameOptions(f -> f.deny()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/tokens").permitAll()
                        // revogação pela posse do refresh token (RFC 7009): logout funciona mesmo com access expirado
                        .requestMatchers(HttpMethod.POST, "/api/v1/token-revocations").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll()
                        .requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> ProblemResponses.write(mapper, res,
                                HttpStatus.UNAUTHORIZED, "unauthenticated", "Não autenticado",
                                "Informe um access token válido no cabeçalho Authorization."))
                        .accessDeniedHandler((req, res, ex) -> ProblemResponses.write(mapper, res,
                                HttpStatus.FORBIDDEN, "forbidden", "Acesso negado",
                                "Seu perfil não permite esta operação.")))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(rateLimitFilter, JwtAuthenticationFilter.class)
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${security.cors.allowed-origins:http://localhost:5173}") List<String> allowedOrigins) {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Correlation-Id"));
        config.setExposedHeaders(List.of("X-Correlation-Id", "Location", "Retry-After"));
        config.setMaxAge(3600L);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
