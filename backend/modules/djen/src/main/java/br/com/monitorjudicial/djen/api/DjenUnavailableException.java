package br.com.monitorjudicial.djen.api;

import br.com.monitorjudicial.shared.domain.ExternalServiceUnavailableException;

/** O DJEN não respondeu adequadamente após as retentativas (RF-014 / RF-020). */
public class DjenUnavailableException extends ExternalServiceUnavailableException {

    public DjenUnavailableException(String message, Throwable cause) {
        super("djen-unavailable", message, cause);
    }
}
