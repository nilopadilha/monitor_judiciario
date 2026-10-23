package br.com.monitorjudicial.djen.infrastructure;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parâmetros do adapter DJEN. Os caminhos são configuráveis porque o contrato foi modelado a partir da documentação
 * pública e precisa ser validado contra o Swagger antes do go-live (risco R1).
 */
@ConfigurationProperties(prefix = "djen")
public record DjenProperties(String baseUrl, int pageSize, int maxPages, Duration connectTimeout,
        Duration readTimeout, int maxAttempts) {

    public DjenProperties {
        baseUrl = baseUrl == null ? "https://comunicaapi.pje.jus.br" : baseUrl;
        pageSize = pageSize <= 0 ? 100 : pageSize;
        maxPages = maxPages <= 0 ? 50 : maxPages;
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(5) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(30) : readTimeout;
        maxAttempts = maxAttempts <= 0 ? 3 : maxAttempts;
    }
}
