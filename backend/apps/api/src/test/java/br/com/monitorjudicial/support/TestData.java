package br.com.monitorjudicial.support;

import java.math.BigInteger;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/** Gera documentos e números CNJ válidos e únicos por execução de teste. */
public final class TestData {

    private static final AtomicInteger SEQUENCE = new AtomicInteger(ThreadLocalRandom.current().nextInt(1, 5_000_000));

    private TestData() {
    }

    public static String cnpj() {
        String base = String.format("%08d", ThreadLocalRandom.current().nextInt(10_000_000, 99_999_999)) + "0001";
        int d1 = cnpjDigit(base, new int[] {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        int d2 = cnpjDigit(base + d1, new int[] {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        return base + d1 + d2;
    }

    private static int cnpjDigit(String digits, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += (digits.charAt(i) - '0') * weights[i];
        }
        int rest = sum % 11;
        return rest < 2 ? 0 : 11 - rest;
    }

    /** Número CNJ válido para o tribunal estadual informado pelo código TR (26 = TJSP). */
    public static String processNumber(String tr) {
        String sequence = String.format("%07d", SEQUENCE.incrementAndGet());
        String year = "2026";
        String origin = "0100";
        int dd = 98 - new BigInteger(sequence + year + "8" + tr + origin + "00").mod(BigInteger.valueOf(97)).intValue();
        return sequence + "-" + String.format("%02d", dd) + "." + year + ".8." + tr + "." + origin;
    }

    public static String digits(String value) {
        return value.replaceAll("\\D", "");
    }
}
