package br.com.monitorjudicial;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Composição do monólito modular: o pacote raiz cobre todos os módulos (componentes, entidades, repositórios e
 * clientes Feign).
 */
@SpringBootApplication
@EnableScheduling
@EnableCaching
@EnableFeignClients
public class MonitorJudicialApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonitorJudicialApplication.class, args);
    }
}
