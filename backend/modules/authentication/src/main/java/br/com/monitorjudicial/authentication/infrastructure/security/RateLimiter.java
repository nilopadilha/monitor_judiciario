package br.com.monitorjudicial.authentication.infrastructure.security;

import java.time.Duration;

/** Limitador de taxa por chave em janela fixa. */
public interface RateLimiter {

    /** Registra uma tentativa; retorna o resultado com o tempo até a próxima janela quando bloqueado. */
    Decision tryAcquire(String key, int limit, Duration window);

    record Decision(boolean allowed, long retryAfterSeconds) {
    }
}
