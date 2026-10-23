package br.com.monitorjudicial.monitoring.application;

import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param maxConcurrency chamadas simultâneas ao DJEN (ADR-0005)
 * @param initialLookbackDays janela retroativa na primeira verificação de um processo (premissa P4)
 * @param overlapDays sobreposição com a última verificação, para publicações disponibilizadas com atraso
 */
@ConfigurationProperties(prefix = "monitoring")
public record MonitoringProperties(int maxConcurrency, int initialLookbackDays, int overlapDays, ZoneId zone) {

    public MonitoringProperties {
        maxConcurrency = maxConcurrency <= 0 ? 8 : maxConcurrency;
        initialLookbackDays = initialLookbackDays <= 0 ? 30 : initialLookbackDays;
        overlapDays = overlapDays < 0 ? 1 : overlapDays;
        zone = zone == null ? ZoneId.of("America/Sao_Paulo") : zone;
    }
}
