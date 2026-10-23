package br.com.monitorjudicial.shared.domain;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Gera UUIDv7 (RFC 9562): 48 bits de timestamp em milissegundos seguidos de bits aleatórios.
 * Ordenáveis no tempo, evitam fragmentação de índices B-tree no PostgreSQL.
 */
public final class Ids {

    private static final SecureRandom RANDOM = new SecureRandom();

    private Ids() {
    }

    public static UUID newId() {
        long timestamp = System.currentTimeMillis();
        long randA = RANDOM.nextInt(1 << 12);
        long msb = (timestamp << 16) | (0x7L << 12) | randA;
        long lsb = (RANDOM.nextLong() & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;
        return new UUID(msb, lsb);
    }
}
