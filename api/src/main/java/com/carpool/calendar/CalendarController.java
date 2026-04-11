package com.carpool.calendar;

import com.carpool.booking.SeatBooking;
import com.carpool.booking.SeatBookingRepository;
import com.carpool.group.GroupAccess;
import com.carpool.group.GroupMember;
import com.carpool.ride.Ride;
import com.carpool.ride.RideRepository;
import com.carpool.ride.RideService;
import com.carpool.security.AuthUser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** My upcoming rides in a group as a calendar file. */
@RestController
public class CalendarController {

    static final int DAYS_AHEAD = 28;

    private final GroupAccess access;
    private final RideService rideService;
    private final RideRepository rides;
    private final SeatBookingRepository bookings;
    private final Clock clock;

    public CalendarController(GroupAccess access, RideService rideService, RideRepository rides,
                              SeatBookingRepository bookings, Clock clock) {
        this.access = access;
        this.rideService = rideService;
        this.rides = rides;
        this.bookings = bookings;
        this.clock = clock;
    }

    @GetMapping("/api/groups/{groupId}/calendar.ics")
    @Transactional
    public ResponseEntity<String> calendar(@AuthenticationPrincipal AuthUser auth, @PathVariable Long groupId) {
        GroupMember me = access.requireMember(groupId, auth.id());
        LocalDate today = LocalDate.now(clock.withZone(me.getGroup().getZone()));
        LocalDate end = today.plusDays(DAYS_AHEAD);
        rideService.materialize(me.getGroup(), today, end);

        List<Ride> upcoming = rides.findForGroup(groupId, today, end).stream()
                .filter(r -> !r.isCancelled() && !r.hasDeparted(clock.instant()))
                .toList();
        Map<Long, List<SeatBooking>> byRide = upcoming.isEmpty() ? Map.of()
                : bookings.findActiveForRides(upcoming.stream().map(Ride::getId).toList()).stream()
                .collect(Collectors.groupingBy(b -> b.getRide().getId()));

        List<IcsWriter.Event> events = new ArrayList<>();
        for (Ride ride : upcoming) {
            List<SeatBooking> riders = byRide.getOrDefault(ride.getId(), List.of());
            boolean driving = ride.isDrivenBy(me);
            boolean riding = riders.stream().anyMatch(b -> b.getMember().getId().equals(me.getId()));
            if (!driving && !riding) {
                continue;
            }
            String summary = (driving ? "Drive: " : "Ride: ") + ride.getTrip().getName();
            String description = driving
                    ? riders.size() + " passenger(s): " + riders.stream().map(b -> b.getMember().getDisplayName())
                    .collect(Collectors.joining(", "))
                    : "Driver: " + (ride.hasDriver() ? ride.getDriver().getDisplayName() : "not decided yet");
            events.add(new IcsWriter.Event("ride-" + ride.getId() + "@carpool", ride.getDepartureAt(),
                    ride.getDepartureAt().plus(Duration.ofMinutes(30)), summary,
                    ride.getTrip().getOrigin() + " to " + ride.getTrip().getDestination(), description));
        }
        String body = IcsWriter.write(me.getGroup().getName(), events, clock.instant());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/calendar; charset=utf-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"carpool-rides.ics\"")
                .body(body);
    }
}
