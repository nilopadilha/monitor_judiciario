package br.com.monitorjudicial.shared.domain;

/**
 * Base das exceções de negócio. O código é estável e vira o sufixo do {@code type} no Problem Details.
 */
public abstract class DomainException extends RuntimeException {

    private final String code;

    protected DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
