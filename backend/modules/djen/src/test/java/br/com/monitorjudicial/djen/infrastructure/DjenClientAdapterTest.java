package br.com.monitorjudicial.djen.infrastructure;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.monitorjudicial.djen.api.DjenClient;
import br.com.monitorjudicial.djen.api.DjenCommunication;
import br.com.monitorjudicial.djen.api.DjenSearch;
import br.com.monitorjudicial.djen.api.DjenUnavailableException;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Teste de contrato do adapter DJEN contra um servidor simulado (WireMock). Fixtures modelam o formato público. */
@SpringBootTest(classes = DjenClientAdapterTest.TestApp.class)
class DjenClientAdapterTest {

    static final WireMockServer djen = new WireMockServer(wireMockConfig().dynamicPort());

    @Configuration
    @EnableFeignClients(clients = DjenFeignClient.class)
    @ComponentScan(basePackageClasses = DjenClientAdapter.class)
    @ImportAutoConfiguration({JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
            FeignAutoConfiguration.class})
    static class TestApp {

        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }
    }

    @BeforeAll
    static void start() {
        djen.start();
    }

    @AfterAll
    static void stop() {
        djen.stop();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("djen.base-url", djen::baseUrl);
        registry.add("djen.page-size", () -> 2);
        registry.add("djen.max-attempts", () -> 3);
    }

    @Autowired
    DjenClient client;

    @BeforeEach
    void reset() {
        djen.resetAll();
    }

    @Test
    void searchMapsFieldsAndWalksPagination() {
        djen.stubFor(get(urlPathEqualTo("/api/v1/comunicacao")).withQueryParam("pagina", equalTo("1"))
                .willReturn(okJson(page(3, item(1, "h1"), item(2, "h2")))));
        djen.stubFor(get(urlPathEqualTo("/api/v1/comunicacao")).withQueryParam("pagina", equalTo("2"))
                .willReturn(okJson(page(3, item(3, "h3")))));

        List<DjenCommunication> result = client.searchCommunications(new DjenSearch("10012348320268260100", "TJSP",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 23)));

        assertThat(result).hasSize(3);
        DjenCommunication first = result.getFirst();
        assertThat(first.djenId()).isEqualTo(1);
        assertThat(first.hash()).isEqualTo("h1");
        assertThat(first.courtCode()).isEqualTo("TJSP");
        assertThat(first.availableAt()).isEqualTo(LocalDate.of(2026, 10, 22));
        assertThat(first.processNumber()).isEqualTo("10012348320268260100");
        assertThat(first.documentType()).isEqualTo("Sentença");
        djen.verify(getRequestedFor(urlPathEqualTo("/api/v1/comunicacao"))
                .withQueryParam("numeroProcesso", equalTo("10012348320268260100"))
                .withQueryParam("dataDisponibilizacaoInicio", equalTo("2026-10-01"))
                .withQueryParam("itensPorPagina", equalTo("2")));
    }

    @Test
    void retriesOn429AndThenSucceeds() {
        djen.stubFor(get(urlPathEqualTo("/api/v1/comunicacao")).inScenario("rate").whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(429).withHeader("Retry-After", "0")).willSetStateTo("ok"));
        djen.stubFor(get(urlPathEqualTo("/api/v1/comunicacao")).inScenario("rate").whenScenarioStateIs("ok")
                .willReturn(okJson(page(1, item(9, "h9")))));

        assertThat(client.searchCommunications(new DjenSearch("1", null, null, null))).hasSize(1);
    }

    @Test
    void persistentFailureBecomesDjenUnavailable() {
        djen.stubFor(get(urlPathEqualTo("/api/v1/comunicacao")).willReturn(aResponse().withStatus(503)));
        assertThatThrownBy(() -> client.searchCommunications(new DjenSearch("1", null, null, null)))
                .isInstanceOf(DjenUnavailableException.class)
                .hasMessageContaining("HTTP 503");
    }

    @Test
    void tribunalsCertificateJournalAndSingleCommunication() {
        djen.stubFor(get(urlPathEqualTo("/api/v1/comunicacao/tribunal")).willReturn(okJson("""
                [{"uf":"SP","nomeEstado":"São Paulo","instituicoes":[{"sigla":"tjsp","nome":"TJSP"},{"sigla":null}]},
                 {"uf":"RJ","instituicoes":[{"sigla":"TJRJ","nome":"TJRJ"}]},{"uf":"XX","instituicoes":null}]
                """)));
        djen.stubFor(get(urlPathEqualTo("/api/v1/comunicacao/abc/certidao"))
                .willReturn(aResponse().withHeader("Content-Type", "application/pdf").withBody(new byte[] {37, 80})));
        djen.stubFor(get(urlPathEqualTo("/api/v1/caderno/TJSP/2026-10-22/E")).willReturn(okJson("""
                {"tribunal":"TJSP","data":"2026-10-22","meio":"E","url":"https://x/caderno.zip","total_comunicacoes":12}
                """)));
        djen.stubFor(get(urlPathEqualTo("/api/v1/comunicacao/abc")).willReturn(okJson(page(1, item(5, "abc")))));
        djen.stubFor(get(urlPathEqualTo("/api/v1/comunicacao/nada")).willReturn(aResponse().withStatus(404)));

        assertThat(client.getTribunals()).extracting("code").containsExactly("TJRJ", "TJSP");
        assertThat(client.getCertificate("abc")).containsExactly(37, 80);
        var journal = client.downloadJournal("TJSP", LocalDate.of(2026, 10, 22), "E");
        assertThat(journal.totalCommunications()).isEqualTo(12);
        assertThat(journal.date()).isEqualTo(LocalDate.of(2026, 10, 22));
        assertThat(client.getCommunication("abc")).get().extracting("hash").isEqualTo("abc");
        assertThat(client.getCommunication("nada")).isEmpty();
    }

    @Test
    void mapperAcceptsBrazilianDates() {
        assertThat(DjenMapper.date("22/10/2026")).isEqualTo(LocalDate.of(2026, 10, 22));
        assertThat(DjenMapper.date("2026-10-22T08:00:00")).isEqualTo(LocalDate.of(2026, 10, 22));
        assertThat(DjenMapper.date(" ")).isNull();
        assertThat(DjenMapper.digits(null)).isNull();
    }

    static String page(int count, String... items) {
        return "{\"status\":\"success\",\"message\":\"ok\",\"count\":" + count + ",\"items\":[" + String.join(",", items)
                + "]}";
    }

    static String item(long id, String hash) {
        return """
                {"id":%d,"hash":"%s","siglaTribunal":"TJSP","data_disponibilizacao":"2026-10-22",
                 "tipoComunicacao":"Intimação","tipoDocumento":"Sentença","nomeOrgao":"1ª Vara Cível",
                 "nomeClasse":"Procedimento Comum Cível","texto":"Julgo procedente o pedido.",
                 "numero_processo":"1001234-83.2026.8.26.0100","link":"https://x","campoNovo":true}
                """.formatted(id, hash);
    }
}
