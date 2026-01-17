package com.carpool.common;

import java.util.List;
import java.util.function.Function;

/** One page of a list. {@code page} is zero-based. */
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages, boolean hasNext) {

    public static <T> PageResponse<T> of(List<T> items, int page, int size, long totalItems) {
        int totalPages = (int) (totalItems / size);
        return new PageResponse<>(items, page, size, totalItems, totalPages, page + 1 < totalPages);
    }

    public static <S, T> PageResponse<T> from(org.springframework.data.domain.Page<S> page, Function<S, T> mapper) {
        return of(page.getContent().stream().map(mapper).toList(), page.getNumber(), page.getSize(),
                page.getTotalElements());
    }
}
