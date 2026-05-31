package com.carpool.common;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResponseTest {

    @Test
    void aPartlyFilledLastPageCounts() {
        PageResponse<String> first = PageResponse.of(List.of("a", "b"), 0, 20, 25);

        assertThat(first.totalPages()).isEqualTo(2);
        assertThat(first.hasNext()).isTrue();
    }

    @Test
    void lastPageHasNoNext() {
        PageResponse<String> last = PageResponse.of(List.of("z"), 1, 20, 25);

        assertThat(last.hasNext()).isFalse();
    }

    @Test
    void exactMultiplesDoNotAddAnEmptyPage() {
        assertThat(PageResponse.of(List.of(), 0, 20, 40).totalPages()).isEqualTo(2);
        assertThat(PageResponse.of(List.of(), 1, 20, 40).hasNext()).isFalse();
    }

    @Test
    void emptyListHasNoPages() {
        PageResponse<String> empty = PageResponse.of(List.of(), 0, 20, 0);

        assertThat(empty.totalPages()).isZero();
        assertThat(empty.hasNext()).isFalse();
    }

    @Test
    void fewerItemsThanAPageIsOnePage() {
        assertThat(PageResponse.of(List.of("a"), 0, 20, 3).totalPages()).isEqualTo(1);
    }
}
