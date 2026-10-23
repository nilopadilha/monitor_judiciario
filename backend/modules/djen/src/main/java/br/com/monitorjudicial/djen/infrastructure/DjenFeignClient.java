package br.com.monitorjudicial.djen.infrastructure;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "djen", url = "${djen.base-url:https://comunicaapi.pje.jus.br}",
        configuration = DjenFeignConfiguration.class)
interface DjenFeignClient {

    @GetMapping("${djen.paths.communications:/api/v1/comunicacao}")
    DjenDtos.CommunicationPage searchCommunications(
            @RequestParam(name = "numeroProcesso", required = false) String numeroProcesso,
            @RequestParam(name = "siglaTribunal", required = false) String siglaTribunal,
            @RequestParam(name = "dataDisponibilizacaoInicio", required = false) String inicio,
            @RequestParam(name = "dataDisponibilizacaoFim", required = false) String fim,
            @RequestParam(name = "pagina") int pagina,
            @RequestParam(name = "itensPorPagina") int itensPorPagina);

    @GetMapping("${djen.paths.communication:/api/v1/comunicacao/{hash}}")
    DjenDtos.CommunicationPage getCommunication(@PathVariable("hash") String hash);

    @GetMapping("${djen.paths.certificate:/api/v1/comunicacao/{hash}/certidao}")
    byte[] getCertificate(@PathVariable("hash") String hash);

    @GetMapping("${djen.paths.tribunals:/api/v1/comunicacao/tribunal}")
    List<DjenDtos.TribunalGroup> getTribunals();

    @GetMapping("${djen.paths.journal:/api/v1/caderno/{sigla}/{data}/{meio}}")
    DjenDtos.Journal getJournal(@PathVariable("sigla") String sigla, @PathVariable("data") String data,
            @PathVariable("meio") String meio);
}
