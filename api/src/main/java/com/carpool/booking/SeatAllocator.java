package com.carpool.booking;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Hands out a ride's seats first come, first served. Without a driver nobody has a seat yet; with
 * one, the earliest requests fill the car and the rest wait in order.
 */
public final class SeatAllocator {

    private SeatAllocator() {
    }

    /**
     * Updates the status of every active booking (in request order) and returns the bookings that
     * gained a seat in this pass.
     */
    public static List<SeatBooking> allocate(boolean hasDriver, int seats, List<SeatBooking> active, Instant now) {
        List<SeatBooking> confirmed = new ArrayList<>();
        int free = hasDriver ? seats : 0;
        for (SeatBooking booking : active) {
            BookingStatus next;
            if (!hasDriver) {
                next = BookingStatus.PENDING;
            } else if (free > 0) {
                next = BookingStatus.CONFIRMED;
                free--;
            } else {
                next = BookingStatus.WAITLISTED;
            }
            if (next == BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.CONFIRMED) {
                confirmed.add(booking);
            }
            booking.setStatus(next, now);
        }
        return confirmed;
    }
}
