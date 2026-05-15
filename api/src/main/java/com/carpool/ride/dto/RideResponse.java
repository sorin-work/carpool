package com.carpool.ride.dto;

import com.carpool.booking.BookingStatus;
import com.carpool.booking.SeatBooking;
import com.carpool.ride.Ride;
import com.carpool.ride.RideStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * A ride as one member sees it: {@code myBooking} is that member's seat request, if any.
 */
public record RideResponse(Long id, Long tripId, String tripName, String origin, String destination,
                           LocalDate date, Instant departureAt, RideStatus status, String cancelReason,
                           DriverInfo driver, int seatsOffered, int seatsTaken, int waitlisted,
                           BookingStatus myBooking, BigDecimal distanceKm, String note) {

    public static RideResponse from(Ride ride, List<SeatBooking> active, Long viewerMemberId) {
        var trip = ride.getTrip();
        int taken = (int) active.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED).count();
        int waiting = (int) active.stream().filter(b -> b.getStatus() == BookingStatus.WAITLISTED).count();
        BookingStatus mine = active.stream()
                .filter(b -> b.getMember().getId().equals(viewerMemberId))
                .map(SeatBooking::getStatus)
                .findFirst().orElse(null);
        return new RideResponse(ride.getId(), trip.getId(), trip.getName(), trip.getOrigin(), trip.getDestination(),
                ride.getRideDate(), ride.getDepartureAt(), ride.getStatus(), ride.getCancelReason(),
                DriverInfo.of(ride.getDriver()), ride.getSeatsOffered(), taken, waiting, mine, trip.getDistanceKm(),
                ride.getNote());
    }
}
