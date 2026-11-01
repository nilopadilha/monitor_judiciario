package br.com.monitorjudicial.shared.domain;

/**
 * Exceção lançada quando um serviço externo obrigatório não responde adequadamente (502 Bad Gateway).
 */
public class ExternalServiceUnavailableException extends DomainException {

    public ExternalServiceUnavailableException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
