package br.com.monitorjudicial.shared.application;

/** Página solicitada, com limites para evitar consultas abusivas. */
public record PageQuery(int page, int size) {

    public static final int MAX_SIZE = 100;

    public PageQuery {
        page = Math.max(page, 0);
        size = size <= 0 ? 20 : Math.min(size, MAX_SIZE);
    }

    public static PageQuery of(int page, int size) {
        return new PageQuery(page, size);
    }
}
