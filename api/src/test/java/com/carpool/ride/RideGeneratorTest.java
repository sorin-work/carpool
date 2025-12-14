package com.carpool.ride;

import com.carpool.ride.RideGenerator.PlannedRide;
import com.carpool.trip.Trip;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.assertj.core.api.Assertions.assertThat;

class RideGeneratorTest {

    private static final ZoneId BUCHAREST = ZoneId.of("Europe/Bucharest");

    static Trip trip(Set<DayOfWeek> days, LocalDate startsOn, LocalDate endsOn) {
        return new Trip(null, "School run", "Elm St", "Riverside Primary", LocalTime.of(7, 45), days,
                new BigDecimal("6.40"), startsOn, endsOn, Instant.EPOCH);
    }

    @Test
    void weekdayTripRunsFiveTimesAWeek() {
        Trip trip = trip(EnumSet.range(MONDAY, FRIDAY), LocalDate.of(2026, 1, 1), null);

        List<PlannedRide> rides = RideGenerator.plan(trip, BUCHAREST, LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 16));

        assertThat(rides).hasSize(10);
        assertThat(rides).extracting(PlannedRide::date).doesNotContain(LocalDate.of(2026, 3, 7), LocalDate.of(2026, 3, 8));
    }

    @Test
    void departureIsLocalToTheGroup() {
        Trip trip = trip(EnumSet.of(MONDAY), LocalDate.of(2026, 1, 1), null);

        List<PlannedRide> rides = RideGenerator.plan(trip, BUCHAREST, LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 3));

        // Bucharest is UTC+2 in winter.
        assertThat(rides).containsExactly(new PlannedRide(LocalDate.of(2026, 3, 2), Instant.parse("2026-03-02T05:45:00Z")));
    }

    @Test
    void rangeEndIsExclusive() {
        Trip trip = trip(EnumSet.of(MONDAY), LocalDate.of(2026, 1, 1), null);

        assertThat(RideGenerator.plan(trip, BUCHAREST, LocalDate.of(2026, 2, 23), LocalDate.of(2026, 3, 2))).hasSize(1);
    }

    @Test
    void nothingBeforeTheTripStarts() {
        Trip trip = trip(EnumSet.of(MONDAY, WEDNESDAY, FRIDAY), LocalDate.of(2026, 3, 4), null);

        List<PlannedRide> rides = RideGenerator.plan(trip, BUCHAREST, LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 9));

        assertThat(rides).extracting(PlannedRide::date)
                .containsExactly(LocalDate.of(2026, 3, 4), LocalDate.of(2026, 3, 6));
    }

    @Test
    void nothingAfterTheTripEnds() {
        Trip trip = trip(EnumSet.of(MONDAY, WEDNESDAY), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 7));

        List<PlannedRide> rides = RideGenerator.plan(trip, BUCHAREST, LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 16));

        assertThat(rides).extracting(PlannedRide::date)
                .containsExactly(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 4));
    }

    @Test
    void emptyWhenTheRangeIsOutsideTheTrip() {
        Trip trip = trip(EnumSet.of(MONDAY), LocalDate.of(2026, 5, 1), null);

        assertThat(RideGenerator.plan(trip, BUCHAREST, LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 16))).isEmpty();
    }
}
