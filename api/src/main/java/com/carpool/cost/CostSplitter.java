package com.carpool.cost;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Splits the fuel cost of one ride evenly between everyone in the car, driver included. Each rider
 * owes their share to the driver; the driver's own share is simply not charged to anyone.
 */
public final class CostSplitter {

    private CostSplitter() {
    }

    /** The cost of a ride in cents. */
    public static long rideCostCents(BigDecimal distanceKm, BigDecimal costPerKm) {
        return cents(distanceKm, costPerKm).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    /** What each rider owes the driver for one ride, in cents. */
    public static long riderShareCents(BigDecimal distanceKm, BigDecimal costPerKm, int riders) {
        if (riders <= 0) {
            return 0;
        }
        // Stay in decimal and round once, to the nearest cent: doubles truncated 69.99... down to 69.
        return cents(distanceKm, costPerKm)
                .divide(BigDecimal.valueOf(riders + 1L), 0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private static BigDecimal cents(BigDecimal distanceKm, BigDecimal costPerKm) {
        return distanceKm.multiply(costPerKm).movePointRight(2);
    }

    /**
     * Balance changes for one ride: the driver gains what the riders owe, every rider loses their
     * share. Positive means the member is owed money.
     */
    public static Map<Long, Long> split(BigDecimal distanceKm, BigDecimal costPerKm, Long driverId, List<Long> riderIds) {
        Map<Long, Long> changes = new LinkedHashMap<>();
        long share = riderShareCents(distanceKm, costPerKm, riderIds.size());
        if (share == 0) {
            return changes;
        }
        changes.put(driverId, share * riderIds.size());
        for (Long rider : riderIds) {
            changes.merge(rider, -share, Long::sum);
        }
        return changes;
    }
}
