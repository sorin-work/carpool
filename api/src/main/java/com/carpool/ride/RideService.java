package com.carpool.ride;

import com.carpool.booking.SeatBooking;
import com.carpool.booking.SeatBookingRepository;
import com.carpool.booking.SeatPlanner;
import com.carpool.common.ApiException;
import com.carpool.group.CarpoolGroup;
import com.carpool.group.GroupAccess;
import com.carpool.group.GroupMember;
import com.carpool.notification.NotificationService;
import com.carpool.notification.NotificationType;
import com.carpool.user.User;
import com.carpool.ride.dto.RideResponse;
import com.carpool.trip.Trip;
import com.carpool.trip.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RideService {

    static final int MAX_RANGE_DAYS = 62;

    private final RideRepository rides;
    private final TripRepository trips;
    private final GroupAccess access;
    private final SeatPlanner planner;
    private final SeatBookingRepository bookings;
    private final NotificationService notifications;
    private final Clock clock;

    public RideService(RideRepository rides, TripRepository trips, GroupAccess access, SeatPlanner planner,
                       SeatBookingRepository bookings, NotificationService notifications, Clock clock) {
        this.notifications = notifications;
        this.rides = rides;
        this.trips = trips;
        this.access = access;
        this.planner = planner;
        this.bookings = bookings;
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
        List<Ride> found = rides.findForGroup(groupId, start, end).stream()
                .filter(ride -> ride.getTrip().isActive() || ride.hasDeparted(clock.instant()))
                .toList();
        return views(found, me);
    }

    @Transactional(readOnly = true)
    public RideResponse get(Long rideId, Long userId) {
        Ride ride = load(rideId);
        GroupMember me = access.requireMember(ride.getTrip().getGroup().getId(), userId);
        return view(ride, me);
    }

    @Transactional
    public RideResponse drive(Long rideId, Long userId, Integer seats) {
        Ride ride = load(rideId);
        GroupMember me = access.requireMember(ride.getTrip().getGroup().getId(), userId);
        requireOpen(ride);
        if (!me.isCanDrive()) {
            throw ApiException.badRequest("Set up your car in the group before offering to drive");
        }
        if (ride.hasDriver() && !ride.isDrivenBy(me)) {
            throw ApiException.conflict(ride.getDriver().getDisplayName() + " is already driving");
        }
        int offered = seats != null ? seats : me.getCarSeats();
        if (offered > me.getCarSeats()) {
            throw ApiException.badRequest("Your car only has " + me.getCarSeats() + " free seats");
        }
        ride.assignDriver(me, offered);
        planner.rebalance(ride, me);
        return view(ride, me);
    }

    @Transactional
    public RideResponse stopDriving(Long rideId, Long userId) {
        Ride ride = load(rideId);
        GroupMember me = access.requireMember(ride.getTrip().getGroup().getId(), userId);
        requireOpen(ride);
        if (!ride.isDrivenBy(me) && !me.getRole().canManage()) {
            throw ApiException.forbidden("Only the driver or an admin can do this");
        }
        GroupMember previous = ride.getDriver();
        ride.clearDriver();
        planner.rebalance(ride, me);
        if (previous != null) {
            String message = RideMessages.bold(previous.getDisplayName()) + " can no longer drive "
                    + RideMessages.describe(ride) + ". Your seat is on hold until someone else takes it.";
            notifications.notifyAll(affected(ride, me), NotificationType.DRIVER_LEFT, message, RideMessages.link(ride));
        }
        return view(ride, me);
    }

    @Transactional
    public RideResponse cancel(Long rideId, Long userId, String reason) {
        Ride ride = load(rideId);
        GroupMember me = access.requireMember(ride.getTrip().getGroup().getId(), userId);
        if (!ride.isDrivenBy(me) && !me.getRole().canManage()) {
            throw ApiException.forbidden("Only the driver or an admin can cancel a ride");
        }
        requireOpen(ride);
        String why = reason == null || reason.isBlank() ? null : reason.trim();
        ride.cancel(why);

        String message = RideMessages.describe(ride) + " was cancelled by " + RideMessages.bold(me.getDisplayName())
                + (why != null ? ": " + why : ".");
        notifications.notifyAll(affected(ride, me), NotificationType.RIDE_CANCELLED, message, RideMessages.link(ride));
        return view(ride, me);
    }

    @Transactional
    public RideResponse reinstate(Long rideId, Long userId) {
        Ride ride = load(rideId);
        GroupMember me = access.requireManager(ride.getTrip().getGroup().getId(), userId);
        if (!ride.isCancelled()) {
            throw ApiException.conflict("This ride is not cancelled");
        }
        if (ride.hasDeparted(clock.instant())) {
            throw ApiException.conflict("This ride has already left");
        }
        ride.reinstate();
        String message = RideMessages.describe(ride) + " is back on.";
        notifications.notifyAll(affected(ride, me), NotificationType.RIDE_REINSTATED, message, RideMessages.link(ride));
        return view(ride, me);
    }

    /** The driver and everyone holding or waiting for a seat, except whoever made the change. */
    private List<User> affected(Ride ride, GroupMember actor) {
        List<GroupMember> people = new ArrayList<>();
        if (ride.hasDriver()) {
            people.add(ride.getDriver());
        }
        bookings.findActiveForRide(ride.getId()).forEach(b -> people.add(b.getMember()));
        return people.stream()
                .filter(m -> !m.getId().equals(actor.getId()))
                .map(GroupMember::getUser)
                .distinct()
                .toList();
    }

    RideResponse view(Ride ride, GroupMember viewer) {
        return views(List.of(ride), viewer).get(0);
    }

    List<RideResponse> views(List<Ride> list, GroupMember viewer) {
        if (list.isEmpty()) {
            return List.of();
        }
        Map<Long, List<SeatBooking>> byRide = bookings.findActiveForRides(list.stream().map(Ride::getId).toList())
                .stream().collect(Collectors.groupingBy(b -> b.getRide().getId()));
        return list.stream()
                .map(ride -> RideResponse.from(ride, byRide.getOrDefault(ride.getId(), List.of()), viewer.getId()))
                .toList();
    }

    private void requireOpen(Ride ride) {
        RideRules.requireOpen(ride, clock.instant());
    }

    /** Creates the rows for any ride in the range that has not been stored yet. */
    public void materialize(CarpoolGroup group, LocalDate from, LocalDate to) {
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
