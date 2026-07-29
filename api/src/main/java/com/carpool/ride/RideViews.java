package com.carpool.ride;

import com.carpool.booking.SeatBooking;
import com.carpool.booking.SeatBookingRepository;
import com.carpool.group.GroupMember;
import com.carpool.ride.dto.RideResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Builds rides as one member sees them, loading the bookings of a whole list in one query. */
@Component
public class RideViews {

    private final SeatBookingRepository bookings;

    public RideViews(SeatBookingRepository bookings) {
        this.bookings = bookings;
    }

    public RideResponse of(Ride ride, GroupMember viewer) {
        return of(List.of(ride), viewer).get(0);
    }

    public List<RideResponse> of(List<Ride> rides, GroupMember viewer) {
        if (rides.isEmpty()) {
            return List.of();
        }
        Map<Long, List<SeatBooking>> byRide = bookings.findActiveForRides(rides.stream().map(Ride::getId).toList())
                .stream().collect(Collectors.groupingBy(b -> b.getRide().getId()));
        return rides.stream()
                .map(ride -> RideResponse.from(ride, byRide.getOrDefault(ride.getId(), List.of()), viewer.getId()))
                .toList();
    }
}
