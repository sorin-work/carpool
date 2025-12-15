package com.carpool.ride;

import com.carpool.common.ApiException;
import com.carpool.group.CarpoolGroup;
import com.carpool.group.GroupAccess;
import com.carpool.group.GroupMember;
import com.carpool.ride.dto.RideResponse;
import com.carpool.trip.Trip;
import com.carpool.trip.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RideService {

    static final int MAX_RANGE_DAYS = 62;

    private final RideRepository rides;
    private final TripRepository trips;
    private final GroupAccess access;
    private final Clock clock;

    public RideService(RideRepository rides, TripRepository trips, GroupAccess access, Clock clock) {
        this.rides = rides;
        this.trips = trips;
        this.access = access;
        this.clock = clock;
    }

    /** The group's rides from {@code from} (inclusive) to {@code to} (exclusive), oldest first. */
    @Transactional
    public List<RideResponse> schedule(Long groupId, Long userId, LocalDate from, LocalDate to) {
        GroupMember me = access.requireMember(groupId, userId);
        CarpoolGroup group = me.getGroup();
        LocalDate start = from != null ? from : LocalDate.now(clock.withZone(group.getZone()));
        LocalDate end = to != null ? to : start.plusDays(14);
        if (!end.isAfter(start)) {
            throw ApiException.badRequest("'to' must be after 'from'");
        }
        if (ChronoUnit.DAYS.between(start, end) > MAX_RANGE_DAYS) {
            throw ApiException.badRequest("Date range is limited to " + MAX_RANGE_DAYS + " days");
        }
        materialize(group, start, end);
        return rides.findForGroup(groupId, start, end).stream()
                .filter(ride -> ride.getTrip().isActive() || ride.hasDeparted(clock.instant()))
                .map(RideResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RideResponse get(Long rideId, Long userId) {
        Ride ride = load(rideId);
        access.requireMember(ride.getTrip().getGroup().getId(), userId);
        return RideResponse.from(ride);
    }

    /** Creates the rows for any ride in the range that has not been stored yet. */
    void materialize(CarpoolGroup group, LocalDate from, LocalDate to) {
        for (Trip trip : trips.findByGroupIdAndActiveTrue(group.getId())) {
            Set<LocalDate> existing = new HashSet<>(rides.findDatesForTrip(trip.getId(), from, to));
            for (RideGenerator.PlannedRide planned : RideGenerator.plan(trip, group.getZone(), from, to)) {
                if (!existing.contains(planned.date())) {
                    rides.save(new Ride(trip, planned.date(), planned.departureAt(), clock.instant()));
                }
            }
        }
    }

    Ride load(Long rideId) {
        return rides.findWithDetails(rideId).orElseThrow(() -> ApiException.notFound("Ride"));
    }
}
