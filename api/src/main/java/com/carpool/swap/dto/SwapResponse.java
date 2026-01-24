package com.carpool.swap.dto;

import com.carpool.ride.Ride;
import com.carpool.swap.SwapRequest;
import com.carpool.swap.SwapStatus;

import java.time.Instant;
import java.time.LocalDate;

public record SwapResponse(Long id, SwapStatus status, String message, Instant createdAt,
                           RideRef ride, RideRef offeredRide,
                           Long requesterId, String requesterName, Long targetId, String targetName) {

    public record RideRef(Long id, String tripName, LocalDate date, Instant departureAt) {
        static RideRef of(Ride ride) {
            return ride == null ? null
                    : new RideRef(ride.getId(), ride.getTrip().getName(), ride.getRideDate(), ride.getDepartureAt());
        }
    }

    public static SwapResponse from(SwapRequest swap) {
        return new SwapResponse(swap.getId(), swap.getStatus(), swap.getMessage(), swap.getCreatedAt(),
                RideRef.of(swap.getRide()), RideRef.of(swap.getOfferedRide()),
                swap.getRequester().getId(), swap.getRequester().getDisplayName(),
                swap.getTarget().getId(), swap.getTarget().getDisplayName());
    }
}
