package com.carpool.group.dto;

import com.carpool.group.MemberRole;

import java.time.Instant;
import java.time.LocalDate;

/** {@code nextRide} is my next ride in the group, if I drive one or asked for a seat. */
public record GroupSummary(Long id, String name, String description, MemberRole myRole, long memberCount,
                           NextRide nextRide) {

    public record NextRide(Long rideId, LocalDate date, Instant departureAt, String tripName, boolean driving) {
    }
}
