package br.com.monitorjudicial.processes.api;

/** Situação de monitoramento de um processo, fornecida pelo módulo monitoring. */
public record ProcessMonitoringStatus(long unreadPublications, boolean lastCheckFailed, String lastError) {

    public static final ProcessMonitoringStatus NONE = new ProcessMonitoringStatus(0, false, null);
}
