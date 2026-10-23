package br.com.monitorjudicial.shared.domain;

/** Regra de negócio violada pelos dados recebidos (HTTP 422). */
public class BusinessRuleViolationException extends DomainException {

    public BusinessRuleViolationException(String code, String message) {
        super(code, message);
    }
}
