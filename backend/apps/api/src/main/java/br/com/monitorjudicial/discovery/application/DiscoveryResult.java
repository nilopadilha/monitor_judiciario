package br.com.monitorjudicial.discovery.application;

import java.util.List;

/** Resultado da descoberta automática de processos no DJEN por parte (cliente) ou OAB (advogado). */
public record DiscoveryResult(
        int communicationsFound,
        int processesCreated,
        int processesLinked,
        int alreadyRegistered,
        List<String> processNumbers,
        List<String> unmatchedProcessNumbers) {
}
