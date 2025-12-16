package com.carpool.booking;

public enum BookingStatus {
    /** The ride has no driver yet. */
    PENDING,
    CONFIRMED,
    /** The car is full; promoted automatically when a seat frees up. */
    WAITLISTED,
    CANCELLED;

    public boolean isActive() {
        return this != CANCELLED;
    }
}
