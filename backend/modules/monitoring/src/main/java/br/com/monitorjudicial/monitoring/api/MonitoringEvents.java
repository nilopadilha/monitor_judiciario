package br.com.monitorjudicial.monitoring.api;

/** Topologia de mensageria publicada pelo módulo monitoring. */
public final class MonitoringEvents {

    public static final String EXCHANGE = "monitor.events";
    public static final String PROCESS_EVOLUTION_DETECTED = "process.evolution-detected";

    private MonitoringEvents() {
    }
}
