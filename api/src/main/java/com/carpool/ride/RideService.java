package com.carpool.ride;

import com.carpool.booking.SeatBooking;
import com.carpool.booking.SeatBookingRepository;
import com.carpool.booking.SeatPlanner;
import com.carpool.common.ApiException;
import com.carpool.group.CarpoolGroup;
import com.carpool.group.GroupAccess;
import com.carpool.group.GroupMember;
import com.carpool.group.GroupMemberRepository;
import com.carpool.ride.dto.MyRide;
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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
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
    private final GroupMemberRepository members;
    private final Clock clock;

    public RideService(RideRepository rides, TripRepository trips, GroupAccess access, SeatPlanner planner,
                       SeatBookingRepository bookings, NotificationService notifications,
                       GroupMemberRepository members, Clock clock) {
        this.notifications = notifications;
        this.members = members;
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

    /** Rides in the next {@code days} days, across all my groups, that I drive or have a seat on. */
    @Transactional
    public List<MyRide> upcomingFor(Long userId, int days) {
        List<MyRide> result = new ArrayList<>();
        for (GroupMember me : members.findAllForUser(userId)) {
            CarpoolGroup group = me.getGroup();
            LocalDate today = LocalDate.now(clock.withZone(group.getZone()));
            materialize(group, today, today.plusDays(days));
            List<Ride> open = rides.findForGroup(group.getId(), today, today.plusDays(days)).stream()
                    .filter(r -> !r.hasDeparted(clock.instant()) && r.getTrip().isActive())
                    .toList();
            for (RideResponse view : views(open, me)) {
                boolean driving = view.driver() != null && view.driver().memberId().equals(me.getId());
                if (driving || view.myBooking() != null) {
                    result.add(new MyRide(group.getId(), group.getName(), group.getZone().getId(), driving, view));
                }
            }
        }
        result.sort(Comparator.comparing(r -> r.ride().departureAt()));
        return result;
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

    /** Admins can put any member with a car behind the wheel, e.g. after a phone call. */
    @Transactional
    public RideResponse assignDriver(Long rideId, Long userId, Long memberId) {
        Ride ride = load(rideId);
        GroupMember admin = access.requireManager(ride.getTrip().getGroup().getId(), userId);
        requireOpen(ride);
        GroupMember driver = members.findById(memberId)
                .filter(m -> m.getGroup().getId().equals(admin.getGroup().getId()))
                .orElseThrow(() -> ApiException.notFound("Member"));
        if (!driver.isCanDrive()) {
            throw ApiException.badRequest(driver.getDisplayName() + " has no car set up");
        }
        ride.assignDriver(driver, driver.getCarSeats());
        planner.rebalance(ride, admin);
        if (!driver.getId().equals(admin.getId())) {
            notifications.notify(driver.getUser(), NotificationType.DRIVER_ASSIGNED,
                    RideMessages.bold(admin.getDisplayName()) + " put you down to drive " + RideMessages.describe(ride) + ".",
                    RideMessages.link(ride));
        }
        return view(ride, admin);
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
    public RideResponse setNote(Long rideId, Long userId, String note) {
        Ride ride = load(rideId);
        GroupMember me = access.requireMember(ride.getTrip().getGroup().getId(), userId);
        if (!ride.isDrivenBy(me) && !me.getRole().canManage()) {
            throw ApiException.forbidden("Only the driver or an admin can change the note");
        }
        ride.setNote(note == null || note.isBlank() ? null : note.trim());
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

    /** Cancels every ride of a trip between two dates (both included), e.g. for school holidays. */
    @Transactional
    public int cancelRange(Trip trip, GroupMember admin, LocalDate from, LocalDate to, String reason) {
        CarpoolGroup group = trip.getGroup();
        materialize(group, from, to.plusDays(1));
        String why = reason == null || reason.isBlank() ? null : reason.trim();
        int cancelled = 0;
        for (Ride ride : rides.findForGroup(group.getId(), from, to.plusDays(1))) {
            if (!ride.getTrip().getId().equals(trip.getId()) || ride.isCancelled() || ride.hasDeparted(clock.instant())) {
                continue;
            }
            ride.cancel(why);
            String message = RideMessages.describe(ride) + " was cancelled by " + RideMessages.bold(admin.getDisplayName())
                    + (why != null ? ": " + why : ".");
            notifications.notifyAll(affected(ride, admin), NotificationType.RIDE_CANCELLED, message, RideMessages.link(ride));
            cancelled++;
        }
        return cancelled;
    }

    /**
     * Brings rides that are already planned in line with an edited trip: they move to the new time,
     * and dates the trip no longer runs on are cancelled (telling anyone who was on them).
     */
    public void replan(Trip trip) {
        ZoneId zone = trip.getGroup().getZone();
        for (Ride ride : rides.findUpcomingForTrip(trip.getId(), clock.instant())) {
            LocalDate date = ride.getRideDate();
            boolean stillRuns = trip.runsOn(date.getDayOfWeek()) && !date.isBefore(trip.getStartsOn())
                    && (trip.getEndsOn() == null || !date.isAfter(trip.getEndsOn()));
            if (stillRuns || ride.isCancelled()) {
                ride.moveTo(ZonedDateTime.of(date, trip.getDepartureTime(), zone).toInstant());
                continue;
            }
            List<User> people = new ArrayList<>();
            if (ride.hasDriver()) {
                people.add(ride.getDriver().getUser());
            }
            bookings.findActiveForRide(ride.getId()).forEach(b -> people.add(b.getMember().getUser()));
            ride.cancel("The trip no longer runs on this day");
            notifications.notifyAll(people.stream().distinct().toList(), NotificationType.RIDE_CANCELLED,
                    RideMessages.describe(ride) + " no longer runs after a change to the trip.",
                    RideMessages.link(ride));
        }
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
