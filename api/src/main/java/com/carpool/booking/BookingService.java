package com.carpool.booking;

import com.carpool.booking.dto.BookingResponse;
import com.carpool.common.ApiException;
import com.carpool.group.GroupAccess;
import com.carpool.group.GroupMember;
import com.carpool.ride.Ride;
import com.carpool.ride.RideRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
public class BookingService {

    private final SeatBookingRepository bookings;
    private final RideRepository rides;
    private final GroupAccess access;
    private final SeatPlanner planner;
    private final Clock clock;

    public BookingService(SeatBookingRepository bookings, RideRepository rides, GroupAccess access,
                          SeatPlanner planner, Clock clock) {
        this.bookings = bookings;
        this.rides = rides;
        this.access = access;
        this.planner = planner;
        this.clock = clock;
    }

    @Transactional
    public BookingResponse request(Long rideId, Long userId) {
        Ride ride = loadRide(rideId);
        GroupMember me = access.requireMember(ride.getTrip().getGroup().getId(), userId);
        requireOpen(ride);
        if (ride.isDrivenBy(me)) {
            throw ApiException.badRequest("You are driving this ride");
        }
        if (bookings.findActive(rideId, me.getId()).isPresent()) {
            throw ApiException.conflict("You already asked for a seat on this ride");
        }
        SeatBooking booking = bookings.save(new SeatBooking(ride, me, clock.instant()));
        planner.rebalance(ride);
        return BookingResponse.from(booking);
    }

    @Transactional
    public void cancel(Long rideId, Long userId) {
        Ride ride = loadRide(rideId);
        GroupMember me = access.requireMember(ride.getTrip().getGroup().getId(), userId);
        requireOpen(ride);
        SeatBooking booking = bookings.findActive(rideId, me.getId())
                .orElseThrow(() -> ApiException.notFound("Booking"));
        booking.setStatus(BookingStatus.CANCELLED, clock.instant());
        bookings.flush();
        planner.rebalance(ride);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> passengers(Long rideId, Long userId) {
        Ride ride = loadRide(rideId);
        access.requireMember(ride.getTrip().getGroup().getId(), userId);
        return bookings.findActiveForRide(rideId).stream().map(BookingResponse::from).toList();
    }

    private Ride loadRide(Long rideId) {
        return rides.findWithDetails(rideId).orElseThrow(() -> ApiException.notFound("Ride"));
    }

    private void requireOpen(Ride ride) {
        if (ride.isCancelled()) {
            throw ApiException.conflict("This ride was cancelled");
        }
        if (ride.hasDeparted(clock.instant())) {
            throw ApiException.conflict("This ride has already left");
        }
    }
}
