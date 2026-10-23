package br.com.monitorjudicial.monitoring.application;

import br.com.monitorjudicial.monitoring.domain.MovementClassifier;
import br.com.monitorjudicial.monitoring.domain.PublicationComparisonService;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra os serviços de domínio (sem anotações Spring no domínio) e o executor das execuções. */
@Configuration
@EnableConfigurationProperties(MonitoringProperties.class)
class MonitoringDomainConfiguration {

    @Bean
    PublicationComparisonService publicationComparisonService() {
        return new PublicationComparisonService();
    }

    @Bean
    MovementClassifier movementClassifier() {
        return new MovementClassifier();
    }

    @Bean(name = "monitoringLauncher", destroyMethod = "close")
    Executor monitoringLauncher() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
