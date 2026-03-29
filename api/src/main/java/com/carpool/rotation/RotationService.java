package com.carpool.rotation;

import com.carpool.booking.SeatBookingRepository;
import com.carpool.booking.SeatPlanner;
import com.carpool.common.ApiException;
import com.carpool.group.GroupAccess;
import com.carpool.group.GroupMember;
import com.carpool.group.GroupMemberRepository;
import com.carpool.notification.NotificationService;
import com.carpool.notification.NotificationType;
import com.carpool.ride.DriverHistory;
import com.carpool.ride.Ride;
import com.carpool.ride.RideMessages;
import com.carpool.ride.RideRepository;
import com.carpool.ride.RideService;
import com.carpool.rotation.dto.RotationProposal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RotationService {

    /** How far back driving history counts towards fairness. */
    static final int HISTORY_DAYS = 56;
    static final int MAX_RANGE_DAYS = 31;

    private final GroupAccess access;
    private final GroupMemberRepository members;
    private final RideRepository rides;
    private final RideService rideService;
    private final SeatBookingRepository bookings;
    private final SeatPlanner planner;
    private final AbsenceRepository absences;
    private final NotificationService notifications;
    private final Clock clock;

    public RotationService(GroupAccess access, GroupMemberRepository members, RideRepository rides,
                           RideService rideService, SeatBookingRepository bookings, SeatPlanner planner,
                           AbsenceRepository absences, NotificationService notifications, Clock clock) {
        this.absences = absences;
        this.notifications = notifications;
        this.access = access;
        this.members = members;
        this.rides = rides;
        this.rideService = rideService;
        this.bookings = bookings;
        this.planner = planner;
        this.clock = clock;
    }

    @Transactional
    public List<RotationProposal> preview(Long groupId, Long userId, LocalDate from, LocalDate to) {
        GroupMember me = access.requireMember(groupId, userId);
        return plan(me, from, to).proposals();
    }

    @Transactional
    public List<RotationProposal> apply(Long groupId, Long userId, LocalDate from, LocalDate to) {
        GroupMember me = access.requireManager(groupId, userId);
        Plan plan = plan(me, from, to);
        for (Ride ride : plan.rides()) {
            Long memberId = plan.assignment().get(ride.getId());
            if (memberId == null) {
                continue;
            }
            GroupMember driver = plan.drivers().get(memberId);
            ride.assignDriver(driver, driver.getCarSeats());
            planner.rebalance(ride, me);
            if (!driver.getId().equals(me.getId())) {
                notifications.notify(driver.getUser(), NotificationType.DRIVER_ASSIGNED,
                        "The rotation has you driving " + RideMessages.describe(ride) + ".", RideMessages.link(ride));
            }
        }
        return plan.proposals();
    }

    private record Plan(List<Ride> rides, Map<Long, Long> assignment, Map<Long, GroupMember> drivers,
                        List<RotationProposal> proposals) {
    }

    private Plan plan(GroupMember me, LocalDate from, LocalDate to) {
        Long groupId = me.getGroup().getId();
        LocalDate start = from != null ? from : LocalDate.now(clock.withZone(me.getGroup().getZone()));
        LocalDate end = to != null ? to : start.plusDays(7);
        if (!end.isAfter(start) || ChronoUnit.DAYS.between(start, end) > MAX_RANGE_DAYS) {
            throw ApiException.badRequest("Pick a range of 1 to " + MAX_RANGE_DAYS + " days");
        }
        rideService.materialize(me.getGroup(), start, end);

        List<Ride> open = rides.findForGroup(groupId, start, end).stream()
                .filter(r -> !r.hasDriver() && !r.isCancelled() && r.getTrip().isActive())
                .filter(r -> !r.hasDeparted(clock.instant()))
                .toList();
        Map<Long, Long> riders = open.isEmpty() ? Map.of() : bookings.findActiveForRides(open.stream().map(Ride::getId).toList())
                .stream().collect(Collectors.groupingBy(b -> b.getRide().getId(), Collectors.counting()));

        Map<Long, GroupMember> drivers = members.findAllInGroup(groupId).stream()
                .filter(GroupMember::isCanDrive)
                .collect(Collectors.toMap(GroupMember::getId, Function.identity()));
        Map<Long, DriverHistory> history = rides.findDriverHistory(groupId, start.minusDays(HISTORY_DAYS)).stream()
                .collect(Collectors.toMap(DriverHistory::memberId, Function.identity()));

        Map<Long, Set<LocalDate>> away = new HashMap<>();
        for (Absence absence : absences.findOverlapping(groupId, start, end)) {
            Set<LocalDate> days = away.computeIfAbsent(absence.getMember().getId(), id -> new HashSet<>());
            for (LocalDate d = start; d.isBefore(end); d = d.plusDays(1)) {
                if (absence.covers(d)) {
                    days.add(d);
                }
            }
        }

        List<FairRotation.Candidate> candidates = new ArrayList<>();
        for (GroupMember driver : drivers.values()) {
            DriverHistory h = history.get(driver.getId());
            candidates.add(new FairRotation.Candidate(driver.getId(), driver.getCarSeats(),
                    h == null ? 0 : (int) h.drives(), h == null ? null : h.lastDrove(),
                    away.getOrDefault(driver.getId(), Set.of())));
        }
        List<FairRotation.Slot> slots = open.stream()
                .map(r -> new FairRotation.Slot(r.getId(), r.getRideDate(), riders.getOrDefault(r.getId(), 0L).intValue()))
                .toList();
        Map<Long, Long> assignment = FairRotation.assign(slots, candidates);

        List<RotationProposal> proposals = open.stream().map(r -> {
            GroupMember driver = drivers.get(assignment.get(r.getId()));
            return new RotationProposal(r.getId(), r.getTrip().getName(), r.getRideDate(), r.getDepartureAt(),
                    driver == null ? null : driver.getId(), driver == null ? null : driver.getDisplayName());
        }).toList();
        return new Plan(open, assignment, drivers, proposals);
    }
}
