package br.com.monitorjudicial.shared.domain;

/** Conflito com o estado atual, como duplicidade ou execução em andamento (HTTP 409). */
public class ConflictException extends DomainException {

    public ConflictException(String code, String message) {
        super(code, message);
    }
}
