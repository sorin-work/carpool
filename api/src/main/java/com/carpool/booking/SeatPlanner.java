package com.carpool.booking;

import com.carpool.ride.Ride;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;

/** Re-runs seat allocation whenever a ride's driver, seats or bookings change. */
@Component
public class SeatPlanner {

    private final SeatBookingRepository bookings;
    private final Clock clock;

    public SeatPlanner(SeatBookingRepository bookings, Clock clock) {
        this.bookings = bookings;
        this.clock = clock;
    }

    public List<SeatBooking> rebalance(Ride ride) {
        List<SeatBooking> active = bookings.findActiveForRide(ride.getId());
        return SeatAllocator.allocate(ride.hasDriver(), ride.getSeatsOffered(), active, clock.instant());
    }
}
