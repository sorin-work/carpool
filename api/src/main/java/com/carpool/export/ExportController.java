package com.carpool.export;

import com.carpool.booking.BookingStatus;
import com.carpool.booking.SeatBooking;
import com.carpool.booking.SeatBookingRepository;
import com.carpool.common.ApiException;
import com.carpool.group.GroupAccess;
import com.carpool.group.GroupMember;
import com.carpool.ride.Ride;
import com.carpool.ride.RideRepository;
import com.carpool.security.AuthUser;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Ride history as a spreadsheet, for groups that settle up outside the app. */
@RestController
public class ExportController {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final GroupAccess access;
    private final RideRepository rides;
    private final SeatBookingRepository bookings;
    private final Clock clock;

    public ExportController(GroupAccess access, RideRepository rides, SeatBookingRepository bookings, Clock clock) {
        this.access = access;
        this.rides = rides;
        this.bookings = bookings;
        this.clock = clock;
    }

    @GetMapping("/api/groups/{groupId}/rides.csv")
    @Transactional(readOnly = true)
    public ResponseEntity<String> rides(@AuthenticationPrincipal AuthUser auth, @PathVariable Long groupId,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        GroupMember me = access.requireMember(groupId, auth.id());
        if (!to.isAfter(from) || from.plusYears(1).isBefore(to)) {
            throw ApiException.badRequest("Pick a range of up to one year");
        }
        List<Ride> done = rides.findCompleted(groupId, from, to, clock.instant());
        Map<Long, List<SeatBooking>> riders = done.isEmpty() ? Map.of()
                : bookings.findActiveForRides(done.stream().map(Ride::getId).toList()).stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .collect(Collectors.groupingBy(b -> b.getRide().getId()));

        CsvWriter csv = new CsvWriter().row(List.of("date", "time", "trip", "distance_km", "driver", "riders"));
        for (Ride ride : done) {
            String names = riders.getOrDefault(ride.getId(), List.of()).stream()
                    .map(b -> b.getMember().getDisplayName()).collect(Collectors.joining("; "));
            csv.row(List.of(ride.getRideDate(), TIME.format(ride.getDepartureAt().atZone(me.getGroup().getZone())),
                    ride.getTrip().getName(), ride.getTrip().getDistanceKm(), ride.getDriver().getDisplayName(), names));
        }
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"rides-" + from + "-to-" + to + ".csv\"")
                .body(csv.toString());
    }
}
