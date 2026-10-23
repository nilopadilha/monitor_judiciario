package br.com.monitorjudicial.djen.infrastructure;

import br.com.monitorjudicial.djen.api.DjenCommunication;
import br.com.monitorjudicial.djen.api.DjenJournal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/** Tradução do formato DJEN para o modelo da porta (MapStruct entre records). */
@Mapper
interface DjenMapper {

    @Mapping(target = "djenId", source = "id")
    @Mapping(target = "courtCode", source = "siglaTribunal")
    @Mapping(target = "availableAt", source = "dataDisponibilizacao", qualifiedByName = "date")
    @Mapping(target = "communicationType", source = "tipoComunicacao")
    @Mapping(target = "documentType", source = "tipoDocumento")
    @Mapping(target = "organName", source = "nomeOrgao")
    @Mapping(target = "className", source = "nomeClasse")
    @Mapping(target = "text", source = "texto")
    @Mapping(target = "processNumber", source = "numeroProcesso", qualifiedByName = "digits")
    DjenCommunication toCommunication(DjenDtos.CommunicationItem item);

    @Mapping(target = "courtCode", source = "tribunal")
    @Mapping(target = "date", source = "data", qualifiedByName = "date")
    @Mapping(target = "medium", source = "meio")
    @Mapping(target = "downloadUrl", source = "url")
    @Mapping(target = "totalCommunications", source = "totalComunicacoes")
    DjenJournal toJournal(DjenDtos.Journal journal);

    @Named("digits")
    static String digits(String value) {
        return value == null ? null : value.replaceAll("\\D", "");
    }

    /** O DJEN usa ISO (2026-10-22) e, em alguns campos, data com hora; ambos são aceitos. */
    @Named("date")
    static LocalDate date(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.length() > 10 ? value.substring(0, 10) : value);
        } catch (DateTimeParseException e) {
            String[] parts = value.substring(0, Math.min(10, value.length())).split("/");
            if (parts.length == 3) {
                return LocalDate.of(Integer.parseInt(parts[2]), Integer.parseInt(parts[1]), Integer.parseInt(parts[0]));
            }
            throw e;
        }
    }
}
