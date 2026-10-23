package br.com.monitorjudicial.authentication.infrastructure.security;

import java.time.Clock;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Janela fixa em Redis (INCR + EXPIRE): consistente entre réplicas. Se o Redis estiver indisponível, a requisição é
 * permitida e o evento é registrado — disponibilidade da API acima do rate limit.
 */
@Component
class RedisRateLimiter implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiter.class);

    private final StringRedisTemplate redis;
    private final Clock clock;

    RedisRateLimiter(StringRedisTemplate redis, Clock clock) {
        this.redis = redis;
        this.clock = clock;
    }

    @Override
    public Decision tryAcquire(String key, int limit, Duration window) {
        long windowSeconds = window.toSeconds();
        long nowSeconds = clock.instant().getEpochSecond();
        long windowIndex = nowSeconds / windowSeconds;
        String redisKey = "rl:" + key + ":" + windowIndex;
        try {
            Long count = redis.opsForValue().increment(redisKey);
            if (count != null && count == 1L) {
                redis.expire(redisKey, window.plusSeconds(1));
            }
            if (count != null && count > limit) {
                return new Decision(false, (windowIndex + 1) * windowSeconds - nowSeconds);
            }
            return new Decision(true, 0);
        } catch (RuntimeException e) {
            log.warn("Rate limiter indisponível; requisição permitida: {}", e.getMessage());
            return new Decision(true, 0);
        }
    }
}
