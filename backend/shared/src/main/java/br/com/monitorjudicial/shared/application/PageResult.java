package br.com.monitorjudicial.shared.application;

import java.util.List;
import java.util.function.Function;

/** Envelope de paginação exposto pela API: {content, page, size, totalElements, totalPages}. */
public record PageResult<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResult<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PageResult<>(List.copyOf(content), page, size, totalElements, totalPages);
    }

    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(content.stream().map(mapper).toList(), page, size, totalElements, totalPages);
    }
}
