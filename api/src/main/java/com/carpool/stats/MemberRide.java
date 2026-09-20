package com.carpool.stats;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A ride a member took part in, as driver or as rider. */
public record MemberRide(Long rideId, LocalDate date, String tripName, boolean driver, BigDecimal distanceKm) {
}
