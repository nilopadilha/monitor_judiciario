package br.com.monitorjudicial.djen.api;

/** O DJEN não respondeu adequadamente após as retentativas. */
public class DjenUnavailableException extends RuntimeException {

    public DjenUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
