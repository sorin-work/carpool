package com.carpool.booking.dto;

import com.carpool.booking.BookingStatus;
import com.carpool.booking.SeatBooking;

import java.time.Instant;

public record BookingResponse(Long id, Long rideId, Long memberId, String displayName, BookingStatus status,
                              Instant createdAt) {

    public static BookingResponse from(SeatBooking booking) {
        return new BookingResponse(booking.getId(), booking.getRide().getId(), booking.getMember().getId(),
                booking.getMember().getDisplayName(), booking.getStatus(), booking.getCreatedAt());
    }
}
