package br.com.monitorjudicial.monitoring.domain.port;

import br.com.monitorjudicial.monitoring.domain.OutboxEvent;

public interface OutboxRepository {

    void append(OutboxEvent event);
}
