package com.carpool.ride;

import com.carpool.trip.Trip;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/** Expands a trip's weekly pattern into the concrete dates it runs on. */
public final class RideGenerator {

    public record PlannedRide(LocalDate date, Instant departureAt) {
    }

    private RideGenerator() {
    }

    /**
     * Rides of {@code trip} between {@code from} (inclusive) and {@code to} (exclusive), limited to
     * the trip's own start and end dates. Departure times are local to the group's time zone.
     */
    public static List<PlannedRide> plan(Trip trip, ZoneId zone, LocalDate from, LocalDate to) {
        LocalDate first = from.isBefore(trip.getStartsOn()) ? trip.getStartsOn() : from;
        LocalDate last = trip.getEndsOn() != null && trip.getEndsOn().isBefore(to) ? trip.getEndsOn() : to;
        if (!first.isBefore(last)) {
            return List.of();
        }
        List<PlannedRide> rides = new ArrayList<>();
        for (LocalDate date = first; date.isBefore(last); date = date.plusDays(1)) {
            if (trip.runsOn(date.getDayOfWeek())) {
                // The offset changes with daylight saving time, so resolve it for every date.
                Instant departure = ZonedDateTime.of(date, trip.getDepartureTime(), zone).toInstant();
                rides.add(new PlannedRide(date, departure));
            }
        }
        return rides;
    }
}
