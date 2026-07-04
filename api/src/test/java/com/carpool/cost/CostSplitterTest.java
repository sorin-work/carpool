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

    @Test
    void roundsSharesToTheNearestCent() {
        // 12.3 km at 0.17 = 2.091, a third each = 0.697: riders pay 0.70, not 0.69.
        assertThat(CostSplitter.riderShareCents(new BigDecimal("12.3"), new BigDecimal("0.17"), 2)).isEqualTo(70);
    }

    @Test
    void sharesThatAreExactInDecimalStayExact() {
        // 0.1 + 0.2 style trouble: 7 km at 0.3 = 2.10, two people = 1.05 each.
        assertThat(CostSplitter.riderShareCents(new BigDecimal("7"), new BigDecimal("0.3"), 1)).isEqualTo(105);
        assertThat(CostSplitter.rideCostCents(new BigDecimal("6.4"), new BigDecimal("0.185"))).isEqualTo(118);
    }
}
