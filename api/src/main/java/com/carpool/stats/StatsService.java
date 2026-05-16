package com.carpool.stats;

import com.carpool.booking.BookingStatus;
import com.carpool.booking.SeatBooking;
import com.carpool.booking.SeatBookingRepository;
import com.carpool.common.ApiException;
import com.carpool.group.GroupAccess;
import com.carpool.group.GroupMember;
import com.carpool.group.GroupMemberRepository;
import com.carpool.ride.Ride;
import com.carpool.ride.RideRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StatsService {

    static final int DEFAULT_DAYS = 90;

    private final GroupAccess access;
    private final GroupMemberRepository members;
    private final RideRepository rides;
    private final SeatBookingRepository bookings;
    private final Clock clock;

    public StatsService(GroupAccess access, GroupMemberRepository members, RideRepository rides,
                        SeatBookingRepository bookings, Clock clock) {
        this.access = access;
        this.members = members;
        this.rides = rides;
        this.bookings = bookings;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public GroupStats forGroup(Long groupId, Long userId, LocalDate from, LocalDate to) {
        GroupMember me = access.requireMember(groupId, userId);
        LocalDate end = to != null ? to : LocalDate.now(clock.withZone(me.getGroup().getZone())).plusDays(1);
        LocalDate start = from != null ? from : end.minusDays(DEFAULT_DAYS);
        if (!end.isAfter(start)) {
            throw ApiException.badRequest("'to' must be after 'from'");
        }

        Map<Long, Tally> tallies = new LinkedHashMap<>();
        for (GroupMember member : members.findAllInGroup(groupId)) {
            tallies.put(member.getId(), new Tally(member));
        }

        List<Ride> completed = rides.findCompleted(groupId, start, end, clock.instant());
        // One query for every booking in the period instead of one per ride.
        Map<Long, List<SeatBooking>> confirmedByRide = completed.isEmpty() ? Map.of()
                : bookings.findActiveForRides(completed.stream().map(Ride::getId).toList()).stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .collect(Collectors.groupingBy(b -> b.getRide().getId()));
        for (Ride ride : completed) {
            BigDecimal km = ride.getTrip().getDistanceKm();
            List<SeatBooking> riders = confirmedByRide.getOrDefault(ride.getId(), List.of());
            Tally driver = tallies.get(ride.getDriver().getId());
            if (driver != null) {
                driver.drives++;
                driver.kmDriven = driver.kmDriven.add(km);
                driver.passengers += riders.size();
                if (driver.lastDrove == null || ride.getRideDate().isAfter(driver.lastDrove)) {
                    driver.lastDrove = ride.getRideDate();
                }
            }
            for (SeatBooking booking : riders) {
                Tally rider = tallies.get(booking.getMember().getId());
                if (rider != null) {
                    rider.rides++;
                    rider.kmRidden = rider.kmRidden.add(km);
                }
            }
        }

        List<MemberStats> result = tallies.values().stream()
                .map(Tally::toStats)
                .sorted(Comparator.comparingInt(MemberStats::drives).reversed()
                        .thenComparing(MemberStats::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        int cancelled = rides.countCancelled(groupId, start, end, clock.instant());
        return new GroupStats(start, end, completed.size(), cancelled, result);
    }

    private static final class Tally {
        final GroupMember member;
        int drives;
        int rides;
        int passengers;
        BigDecimal kmDriven = BigDecimal.ZERO;
        BigDecimal kmRidden = BigDecimal.ZERO;
        LocalDate lastDrove;

        Tally(GroupMember member) {
            this.member = member;
        }

        MemberStats toStats() {
            int total = drives + rides;
            int share = total == 0 ? 0 : Math.round(drives * 100f / total);
            return new MemberStats(member.getId(), member.getDisplayName(), drives, kmDriven, rides, kmRidden,
                    passengers, lastDrove, share);
        }
    }
}
