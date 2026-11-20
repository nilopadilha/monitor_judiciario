package br.com.monitorjudicial.monitoring.infrastructure.datajud;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuração da API Pública do DataJud (CNJ). Ativada somente com {@code datajud.enabled=true}. */
@ConfigurationProperties(prefix = "datajud")
public record DataJudProperties(boolean enabled, String baseUrl, String apiKey, int maxHits) {

    public DataJudProperties {
        baseUrl = baseUrl == null || baseUrl.isBlank() ? "https://api-publica.datajud.cnj.jus.br" : baseUrl.strip();
        apiKey = apiKey == null ? "" : apiKey.strip();
        maxHits = maxHits <= 0 ? 20 : Math.min(maxHits, 100);
    }

    public boolean configured() {
        return enabled && !apiKey.isBlank();
    }
}
