package br.com.monitorjudicial.discovery.application;

import br.com.monitorjudicial.clients.api.ClientDirectory;
import br.com.monitorjudicial.clients.api.ClientSummary;
import br.com.monitorjudicial.djen.api.DjenClient;
import br.com.monitorjudicial.djen.api.DjenCommunication;
import br.com.monitorjudicial.djen.api.DjenSearch;
import br.com.monitorjudicial.lawyers.api.LawyerDirectory;
import br.com.monitorjudicial.lawyers.api.LawyerSummary;
import br.com.monitorjudicial.processes.application.ProcessService;
import br.com.monitorjudicial.processes.application.RegisterProcessCommand;
import br.com.monitorjudicial.shared.audit.Auditor;
import br.com.monitorjudicial.shared.domain.BusinessRuleViolationException;
import br.com.monitorjudicial.shared.domain.ConflictException;
import br.com.monitorjudicial.shared.domain.ResourceNotFoundException;
import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Descoberta automática de processos no DJEN por nome da parte (cliente) e por OAB/UF (advogado),
 * com auto-vinculação imediata de processos encontrados.
 */
@Service
public class ProcessDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(ProcessDiscoveryService.class);
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");

    private final DjenClient djen;
    private final ClientDirectory clients;
    private final LawyerDirectory lawyers;
    private final ProcessService processes;
    private final ProcessDiscoveryPort port;
    private final Auditor auditor;
    private final Clock clock;
    private final int lookbackDays;
    private final int maxPages;

    public ProcessDiscoveryService(DjenClient djen, ClientDirectory clients, LawyerDirectory lawyers,
            ProcessService processes, ProcessDiscoveryPort port, Auditor auditor, Clock clock,
            @Value("${monitoring.initial-lookback-days:3650}") int lookbackDays,
            @Value("${discovery.max-pages:3}") int maxPages) {
        this.djen = djen;
        this.clients = clients;
        this.lawyers = lawyers;
        this.processes = processes;
        this.port = port;
        this.auditor = auditor;
        this.clock = clock;
        this.lookbackDays = lookbackDays;
        this.maxPages = maxPages;
    }

    public DiscoveryResult discoverForClient(UUID clientId) {
        ClientSummary client = clients.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("cliente", clientId));
        if (!client.active()) {
            throw new BusinessRuleViolationException("client-inactive",
                    "Não é possível descobrir processos para cliente inativo.");
        }
        LocalDate today = LocalDate.now(clock.withZone(BUSINESS_ZONE));
        LocalDate from = lookbackDays <= 0 ? null : today.minusDays(lookbackDays);
        List<DjenCommunication> communications = djen.searchCommunications(
                DjenSearch.byPartyName(client.name(), from, today, maxPages));

        List<LawyerSummary> activeLawyers = lawyers.findActive();
        Map<String, DjenCommunication> byProcess = deduplicateByProcess(communications);

        int created = 0;
        int alreadyRegistered = 0;
        List<String> createdNumbers = new ArrayList<>();

        for (var entry : byProcess.entrySet()) {
            String digits = entry.getKey();
            DjenCommunication comm = entry.getValue();
            if (port.findByDigits(digits).isPresent()) {
                alreadyRegistered++;
                continue;
            }
            LawyerSummary matchedLawyer = matchLawyer(comm, activeLawyers);
            try {
                var registered = processes.register(new RegisterProcessCommand(
                        client.id(),
                        matchedLawyer == null ? null : matchedLawyer.id(),
                        matchedLawyer == null ? null : matchedLawyer.practiceArea(),
                        digits,
                        subjectFrom(comm),
                        false));
                created++;
                createdNumbers.add(registered.number());
            } catch (ConflictException e) {
                alreadyRegistered++;
            } catch (BusinessRuleViolationException e) {
                log.debug("Processo {} ignorado na descoberta do cliente {}: {}", digits, clientId, e.getMessage());
            }
        }
        auditor.record("DISCOVER_PROCESSES", "client", clientId, Map.of(
                "communicationsFound", communications.size(), "processesCreated", created));
        return new DiscoveryResult(communications.size(), created, 0, alreadyRegistered, createdNumbers, List.of());
    }

    public DiscoveryResult discoverForLawyer(UUID lawyerId) {
        LawyerSummary lawyer = lawyers.findById(lawyerId)
                .orElseThrow(() -> new ResourceNotFoundException("advogado", lawyerId));
        if (!lawyer.active()) {
            throw new BusinessRuleViolationException("lawyer-inactive",
                    "Não é possível descobrir processos para advogado inativo.");
        }
        LocalDate today = LocalDate.now(clock.withZone(BUSINESS_ZONE));
        LocalDate from = lookbackDays <= 0 ? null : today.minusDays(lookbackDays);
        List<DjenCommunication> communications = djen.searchCommunications(
                DjenSearch.byOab(lawyer.oabNumber(), lawyer.oabUf(), from, today, maxPages));

        List<ProcessDiscoveryPort.ActiveClient> activeClients = port.findActiveClients();
        Map<String, DjenCommunication> byProcess = deduplicateByProcess(communications);

        int created = 0;
        int linked = 0;
        int alreadyRegistered = 0;
        List<String> numbers = new ArrayList<>();
        List<String> unmatched = new ArrayList<>();

        for (var entry : byProcess.entrySet()) {
            String digits = entry.getKey();
            DjenCommunication comm = entry.getValue();
            var existing = port.findByDigits(digits);
            if (existing.isPresent()) {
                if (existing.get().lawyerId() == null) {
                    port.assignLawyer(existing.get().id(), lawyer.id(), lawyer.practiceArea());
                    linked++;
                    numbers.add(existing.get().formattedNumber());
                } else {
                    alreadyRegistered++;
                }
                continue;
            }
            var matchedClient = matchClient(comm, activeClients);
            if (matchedClient == null) {
                unmatched.add(digits);
                continue;
            }
            try {
                var registered = processes.register(new RegisterProcessCommand(
                        matchedClient.id(), lawyer.id(), lawyer.practiceArea(), digits, subjectFrom(comm), false));
                created++;
                numbers.add(registered.number());
            } catch (ConflictException e) {
                alreadyRegistered++;
            } catch (BusinessRuleViolationException e) {
                log.debug("Processo {} ignorado na descoberta do advogado {}: {}", digits, lawyerId, e.getMessage());
            }
        }
        auditor.record("DISCOVER_PROCESSES", "lawyer", lawyerId, Map.of(
                "communicationsFound", communications.size(), "processesCreated", created, "processesLinked", linked));
        return new DiscoveryResult(communications.size(), created, linked, alreadyRegistered, numbers, unmatched);
    }

    private static Map<String, DjenCommunication> deduplicateByProcess(List<DjenCommunication> list) {
        Map<String, DjenCommunication> map = new LinkedHashMap<>();
        for (DjenCommunication comm : list) {
            if (comm == null || comm.processNumber() == null) {
                continue;
            }
            String digits = comm.processNumber().replaceAll("\\D", "");
            if (digits.length() == 20) {
                map.putIfAbsent(digits, comm);
            }
        }
        return map;
    }

    private static LawyerSummary matchLawyer(DjenCommunication comm, List<LawyerSummary> activeLawyers) {
        if (activeLawyers.isEmpty()) {
            return null;
        }
        for (LawyerSummary lawyer : activeLawyers) {
            String oabKey = lawyer.oabNumber().toUpperCase(Locale.ROOT) + "/" + lawyer.oabUf().toUpperCase(Locale.ROOT);
            if (comm.lawyerOabs().contains(oabKey)) {
                return lawyer;
            }
        }
        String text = comm.text();
        if (text == null || text.isBlank()) {
            return null;
        }
        String upper = text.toUpperCase(Locale.ROOT);
        for (LawyerSummary lawyer : activeLawyers) {
            if (upper.contains(lawyer.oabNumber()) && upper.contains(lawyer.oabUf())) {
                return lawyer;
            }
        }
        return null;
    }

    private static ProcessDiscoveryPort.ActiveClient matchClient(DjenCommunication comm,
            List<ProcessDiscoveryPort.ActiveClient> activeClients) {
        if (activeClients.isEmpty()) {
            return null;
        }
        List<String> normalizedRecipients = comm.recipientNames().stream()
                .map(ProcessDiscoveryService::normalize)
                .toList();
        String normalizedText = comm.text() == null ? "" : normalize(comm.text());
        for (var client : activeClients) {
            if (client.name() == null || client.name().length() < 4) {
                continue;
            }
            String needle = normalize(client.name());
            if (normalizedRecipients.stream().anyMatch(r -> r.contains(needle) || needle.contains(r) && r.length() >= 4)
                    || (!normalizedText.isEmpty() && normalizedText.contains(needle))) {
                return client;
            }
        }
        return null;
    }

    private static String normalize(String value) {
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).strip();
    }

    private static String subjectFrom(DjenCommunication comm) {
        if (comm.className() != null && !comm.className().isBlank()) {
            String s = comm.className().strip();
            return s.length() > 300 ? s.substring(0, 300) : s;
        }
        return comm.documentType();
    }
}
