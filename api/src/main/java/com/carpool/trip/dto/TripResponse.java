package com.carpool.trip.dto;

import com.carpool.trip.Trip;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record TripResponse(Long id, Long groupId, String name, String origin, String destination,
                           LocalTime departureTime, List<DayOfWeek> days, BigDecimal distanceKm,
                           LocalDate startsOn, LocalDate endsOn, boolean active) {

    public static TripResponse from(Trip trip) {
        return new TripResponse(trip.getId(), trip.getGroup().getId(), trip.getName(), trip.getOrigin(),
                trip.getDestination(), trip.getDepartureTime(), trip.getDays().stream().sorted().toList(),
                trip.getDistanceKm(), trip.getStartsOn(), trip.getEndsOn(), trip.isActive());
    }
}
