package com.carpool.ride.dto;

import com.carpool.ride.Ride;
import com.carpool.ride.RideStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record RideResponse(Long id, Long tripId, String tripName, String origin, String destination,
                           LocalDate date, Instant departureAt, RideStatus status, String cancelReason,
                           DriverInfo driver, int seatsOffered, BigDecimal distanceKm) {

    public static RideResponse from(Ride ride) {
        var trip = ride.getTrip();
        return new RideResponse(ride.getId(), trip.getId(), trip.getName(), trip.getOrigin(), trip.getDestination(),
                ride.getRideDate(), ride.getDepartureAt(), ride.getStatus(), ride.getCancelReason(),
                DriverInfo.of(ride.getDriver()), ride.getSeatsOffered(), trip.getDistanceKm());
    }
}
