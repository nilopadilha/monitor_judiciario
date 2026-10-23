package br.com.monitorjudicial.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfiguration {

    @Bean
    OpenAPI monitorJudicialOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Monitor Judicial API")
                        .version("v1")
                        .description("Monitoramento de processos judiciais via DJEN. Padrões de API do PJe; erros em "
                                + "RFC 9457 Problem Details."))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}
