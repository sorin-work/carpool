package com.carpool.trip;

import com.carpool.common.ApiException;
import com.carpool.group.GroupAccess;
import com.carpool.group.GroupMember;
import com.carpool.trip.dto.TripRequest;
import com.carpool.trip.dto.TripResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
public class TripService {

    private final TripRepository trips;
    private final GroupAccess access;
    private final Clock clock;

    public TripService(TripRepository trips, GroupAccess access, Clock clock) {
        this.trips = trips;
        this.access = access;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<TripResponse> list(Long groupId, Long userId) {
        access.requireMember(groupId, userId);
        return trips.findByGroupIdOrderByDepartureTimeAscNameAsc(groupId).stream().map(TripResponse::from).toList();
    }

    @Transactional
    public TripResponse create(Long groupId, Long userId, TripRequest request) {
        GroupMember admin = access.requireManager(groupId, userId);
        Trip trip = new Trip(admin.getGroup(), request.name().trim(), request.origin().trim(),
                request.destination().trim(), request.departureTime(), request.days(), request.distanceKm(),
                request.startsOn(), request.endsOn(), clock.instant());
        return TripResponse.from(trips.save(trip));
    }

    @Transactional(readOnly = true)
    public TripResponse get(Long tripId, Long userId) {
        Trip trip = load(tripId);
        access.requireMember(trip.getGroup().getId(), userId);
        return TripResponse.from(trip);
    }

    Trip load(Long tripId) {
        return trips.findById(tripId).orElseThrow(() -> ApiException.notFound("Trip"));
    }
}
