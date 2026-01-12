package com.carpool.ride;

import java.time.LocalDate;

/** How often a member drove (or is booked to drive) in a period, and the latest such date. */
public record DriverHistory(Long memberId, long drives, LocalDate lastDrove) {
}
