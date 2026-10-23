package br.com.monitorjudicial.shared.domain;

/** Credenciais ou token inválidos (HTTP 401). */
public class UnauthorizedException extends DomainException {

    public UnauthorizedException(String code, String message) {
        super(code, message);
    }
}
