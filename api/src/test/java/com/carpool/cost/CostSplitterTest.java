package com.carpool.cost;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CostSplitterTest {

    @Test
    void rideCostIsDistanceTimesRate() {
        assertThat(CostSplitter.rideCostCents(new BigDecimal("12.5"), new BigDecimal("0.20"))).isEqualTo(250);
    }

    @Test
    void driverPaysTheirOwnShare() {
        // 20 km at 0.15 = 3.00 split four ways: three riders owe 0.75 each.
        assertThat(CostSplitter.riderShareCents(new BigDecimal("20"), new BigDecimal("0.15"), 3)).isEqualTo(75);
    }

    @Test
    void ridersOweTheDriver() {
        var changes = CostSplitter.split(new BigDecimal("8"), new BigDecimal("0.25"), 1L, List.of(2L, 3L, 4L));

        assertThat(changes).containsEntry(1L, 150L).containsEntry(2L, -50L).containsEntry(3L, -50L).containsEntry(4L, -50L);
        assertThat(changes.values().stream().mapToLong(Long::longValue).sum()).isZero();
    }

    @Test
    void nobodyOwesAnythingForASoloDrive() {
        assertThat(CostSplitter.split(new BigDecimal("8"), new BigDecimal("0.25"), 1L, List.of())).isEmpty();
    }

    @Test
    void freeRidesWhenTheRateIsZero() {
        assertThat(CostSplitter.split(new BigDecimal("8"), BigDecimal.ZERO, 1L, List.of(2L))).isEmpty();
    }
}
