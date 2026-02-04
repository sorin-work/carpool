package com.carpool.cost;

import com.carpool.booking.BookingStatus;
import com.carpool.booking.SeatBooking;
import com.carpool.booking.SeatBookingRepository;
import com.carpool.common.ApiException;
import com.carpool.group.CarpoolGroup;
import com.carpool.group.GroupAccess;
import com.carpool.group.GroupMember;
import com.carpool.group.GroupMemberRepository;
import com.carpool.ride.Ride;
import com.carpool.ride.RideRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CostService {

    private final GroupAccess access;
    private final GroupMemberRepository members;
    private final RideRepository rides;
    private final SeatBookingRepository bookings;
    private final Clock clock;

    public CostService(GroupAccess access, GroupMemberRepository members, RideRepository rides,
                       SeatBookingRepository bookings, Clock clock) {
        this.access = access;
        this.members = members;
        this.rides = rides;
        this.bookings = bookings;
        this.clock = clock;
    }

    /** Fuel costs of the rides that took place in [from, to), defaulting to the current month. */
    @Transactional(readOnly = true)
    public CostReport report(Long groupId, Long userId, LocalDate from, LocalDate to) {
        GroupMember me = access.requireMember(groupId, userId);
        CarpoolGroup group = me.getGroup();
        LocalDate today = LocalDate.now(clock.withZone(group.getZone()));
        LocalDate start = from != null ? from : today.withDayOfMonth(1);
        LocalDate end = to != null ? to : start.plusMonths(1);
        if (!end.isAfter(start)) {
            throw ApiException.badRequest("'to' must be after 'from'");
        }

        List<Ride> completed = rides.findCompleted(groupId, start, end, clock.instant());
        Map<Long, List<Long>> ridersByRide = completed.isEmpty() ? Map.of()
                : bookings.findActiveForRides(completed.stream().map(Ride::getId).toList()).stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .collect(Collectors.groupingBy(b -> b.getRide().getId(),
                        Collectors.mapping(SeatBooking::getMember, Collectors.mapping(GroupMember::getId, Collectors.toList()))));

        Map<Long, Long> earned = new HashMap<>();
        Map<Long, Long> spent = new HashMap<>();
        long total = 0;
        for (Ride ride : completed) {
            total += CostSplitter.rideCostCents(ride.getTrip().getDistanceKm(), group.getCostPerKm());
            List<Long> riders = ridersByRide.getOrDefault(ride.getId(), List.of());
            CostSplitter.split(ride.getTrip().getDistanceKm(), group.getCostPerKm(), ride.getDriver().getId(), riders)
                    .forEach((memberId, change) -> {
                        if (change > 0) {
                            earned.merge(memberId, change, Long::sum);
                        } else {
                            spent.merge(memberId, -change, Long::sum);
                        }
                    });
        }

        List<MemberBalance> balances = members.findAllInGroup(groupId).stream()
                .map(m -> {
                    long in = earned.getOrDefault(m.getId(), 0L);
                    long out = spent.getOrDefault(m.getId(), 0L);
                    return new MemberBalance(m.getId(), m.getDisplayName(), in, out, in - out);
                })
                .sorted(Comparator.comparingLong(MemberBalance::balanceCents).reversed())
                .toList();
        return new CostReport(start, end, group.getCurrency(), group.getCostPerKm(), total, balances,
                SettlementPlanner.settle(balances));
    }
}
