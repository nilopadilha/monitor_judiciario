package br.com.monitorjudicial.monitoring.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Impressão digital do conteúdo: SHA-256 do texto normalizado (Unicode NFKC, minúsculas, espaços colapsados e sem
 * espaço antes de pontuação, sem marcação HTML). Duas comunicações com o mesmo fingerprint e hashes diferentes são uma republicação (Q5).
 */
public final class ContentFingerprint {

    private ContentFingerprint() {
    }

    public static String of(String text) {
        String normalized = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFKC)
                .replaceAll("<[^>]+>", " ")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .replaceAll(" ([.,;:!?)])", "$1")
                .strip();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
