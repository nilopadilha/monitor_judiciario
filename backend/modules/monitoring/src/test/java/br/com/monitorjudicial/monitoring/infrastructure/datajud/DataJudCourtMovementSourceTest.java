package br.com.monitorjudicial.monitoring.infrastructure.datajud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import br.com.monitorjudicial.monitoring.domain.CandidatePublication;
import br.com.monitorjudicial.monitoring.domain.PublicationSource;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class DataJudCourtMovementSourceTest {

    @Test
    void queriesCourtAliasFiltersByDateWindowAndMapsMovements() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var meters = new SimpleMeterRegistry();
        var props = new DataJudProperties(true, "https://datajud.test", "secret-key", 10);

        String response = """
                {
                  "hits": {
                    "hits": [
                      {
                        "_source": {
                          "orgaoJulgador": { "nome": "1ª Vara Cível" },
                          "movimentos": [
                            {
                              "codigo": 219,
                              "nome": "Sentença",
                              "dataHora": "2026-10-22T14:30:00.000Z",
                              "complementosTabelados": [
                                { "descricao": "tipo_de_decisao", "nome": "Procedente" }
                              ]
                            },
                            {
                              "codigo": 50,
                              "nome": "Distribuição",
                              "dataHora": "2026-09-01T10:00:00.000Z"
                            }
                          ]
                        }
                      }
                    ]
                  }
                }
                """;
        server.expect(requestTo("https://datajud.test/api_publica_tjsp/_search"))
                .andExpect(header("Authorization", "APIKey secret-key"))
                .andExpect(jsonPath("$.query.match.numeroProcesso").value("10012348320268260100"))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        var source = new DataJudCourtMovementSource(props, builder, meters);
        List<CandidatePublication> result = source.movementsOf("1001234-83.2026.8.26.0100", "TJSP",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 23));

        server.verify();
        assertThat(result).hasSize(1);
        CandidatePublication mov = result.getFirst();
        assertThat(mov.source()).isEqualTo(PublicationSource.DATAJUD);
        assertThat(mov.documentType()).isEqualTo("Sentença");
        assertThat(mov.organName()).isEqualTo("1ª Vara Cível");
        assertThat(mov.content()).contains("Sentença", "tipo_de_decisao: Procedente");
        assertThat(mov.publicationHash()).startsWith("datajud:TJSP:219:");
        assertThat(meters.counter("datajud.requests", "court", "TJSP", "outcome", "success").count()).isEqualTo(1);
    }

    @Test
    void handlesUnconfiguredAndErrorsGracefully() {
        var meters = new SimpleMeterRegistry();
        var disabled = new DataJudCourtMovementSource(new DataJudProperties(true, null, "", 0),
                RestClient.builder(), meters);
        assertThat(disabled.movementsOf("10012348320268260100", "TJSP", null, null)).isEmpty();

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api-publica.datajud.cnj.jus.br/api_publica_tjrj/_search"))
                .andRespond(withServerError());
        var active = new DataJudCourtMovementSource(new DataJudProperties(true, null, "key", 5), builder, meters);
        assertThat(active.movementsOf("10012348320268260100", "TJRJ", null, null)).isEmpty();
        assertThat(meters.counter("datajud.requests", "court", "TJRJ", "outcome", "500").count()).isEqualTo(1);
    }

    @Test
    void parsesMultipleDateFormatsSafely() {
        assertThat(DataJudCourtMovementSource.parseDate("2026-10-22T14:30:00-03:00"))
                .isEqualTo(LocalDate.of(2026, 10, 22));
        assertThat(DataJudCourtMovementSource.parseDate("2026-10-22")).isEqualTo(LocalDate.of(2026, 10, 22));
        assertThat(DataJudCourtMovementSource.parseDate("invalido")).isNull();
        assertThat(DataJudCourtMovementSource.parseDate(null)).isNull();
    }
}
