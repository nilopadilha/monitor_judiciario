package br.com.monitorjudicial.djen.infrastructure;

import br.com.monitorjudicial.djen.api.DjenCommunication;
import br.com.monitorjudicial.djen.api.DjenJournal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/** Tradução do formato DJEN para o modelo da porta (MapStruct entre records). */
@Mapper
interface DjenMapper {

    @Mapping(target = "djenId", source = "id")
    @Mapping(target = "courtCode", source = "siglaTribunal")
    @Mapping(target = "availableAt", expression = "java(DjenMapper.date(item.resolvedDataDisponibilizacao()))")
    @Mapping(target = "communicationType", source = "tipoComunicacao")
    @Mapping(target = "documentType", source = "tipoDocumento")
    @Mapping(target = "organName", source = "nomeOrgao")
    @Mapping(target = "className", source = "nomeClasse")
    @Mapping(target = "text", source = "texto")
    @Mapping(target = "processNumber", expression = "java(DjenMapper.digits(item.resolvedNumeroProcesso()))")
    @Mapping(target = "recipientNames", expression = "java(DjenMapper.recipients(item.destinatarios()))")
    @Mapping(target = "lawyerOabs", expression = "java(DjenMapper.lawyerOabs(item.destinatarioadvogados()))")
    DjenCommunication toCommunication(DjenDtos.CommunicationItem item);

    @Mapping(target = "courtCode", source = "tribunal")
    @Mapping(target = "date", source = "data", qualifiedByName = "date")
    @Mapping(target = "medium", source = "meio")
    @Mapping(target = "downloadUrl", source = "url")
    @Mapping(target = "totalCommunications", expression = "java(journal.resolvedTotalComunicacoes())")
    DjenJournal toJournal(DjenDtos.Journal journal);

    static List<String> recipients(List<DjenDtos.Destinatario> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        return items.stream()
                .filter(d -> d != null && d.nome() != null && !d.nome().isBlank())
                .map(d -> d.nome().strip())
                .distinct()
                .toList();
    }

    static List<String> lawyerOabs(List<DjenDtos.DestinatarioAdvogado> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        return items.stream()
                .filter(d -> d != null && d.advogado() != null
                        && d.advogado().numeroOab() != null && !d.advogado().numeroOab().isBlank()
                        && d.advogado().ufOab() != null && !d.advogado().ufOab().isBlank())
                .map(d -> d.advogado().numeroOab().strip().toUpperCase(Locale.ROOT)
                        + "/" + d.advogado().ufOab().strip().toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
    }

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
