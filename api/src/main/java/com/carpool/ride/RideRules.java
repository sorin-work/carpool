package com.carpool.ride;

import com.carpool.common.ApiException;

import java.time.Instant;

/** Checks shared by everything that changes a ride. */
public final class RideRules {

    private RideRules() {
    }

    /** Fails unless the ride is still on and has not left yet. */
    public static void requireOpen(Ride ride, Instant now) {
        if (ride.isCancelled()) {
            throw ApiException.conflict("This ride was cancelled");
        }
        if (ride.hasDeparted(now)) {
            throw ApiException.conflict("This ride has already left");
        }
    }
}
