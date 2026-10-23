package br.com.monitorjudicial.monitoring.domain.port;

import java.util.Optional;

/** Exclusão mútua entre réplicas para execuções de monitoramento (ADR-0006). */
public interface MonitoringLock {

    Optional<Handle> tryAcquire();

    interface Handle extends AutoCloseable {

        @Override
        void close();
    }
}
