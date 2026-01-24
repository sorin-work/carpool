package com.carpool.stats;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Driving and riding totals for one member. {@code driveShare} is the percentage of the member's
 * trips (driven + ridden) where they were the driver.
 */
public record MemberStats(Long memberId, String displayName, int drives, BigDecimal kmDriven, int ridesTaken,
                          BigDecimal kmRidden, int passengersCarried, LocalDate lastDrove, int driveShare) {
}
