package com.carpool.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SeatBookingRepository extends JpaRepository<SeatBooking, Long> {

    /** Bookings that still hold or wait for a seat, in the order they were made. */
    @Query("""
            select b from SeatBooking b join fetch b.member m join fetch m.user
            where b.ride.id = :rideId and b.status <> com.carpool.booking.BookingStatus.CANCELLED
            order by b.createdAt, b.id
            """)
    List<SeatBooking> findActiveForRide(Long rideId);

    @Query("""
            select b from SeatBooking b
            where b.ride.id = :rideId and b.member.id = :memberId
              and b.status <> com.carpool.booking.BookingStatus.CANCELLED
            """)
    Optional<SeatBooking> findActive(Long rideId, Long memberId);
}
