package com.carpool.booking;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import static com.carpool.booking.BookingStatus.CONFIRMED;
import static com.carpool.booking.BookingStatus.PENDING;
import static com.carpool.booking.BookingStatus.WAITLISTED;
import static org.assertj.core.api.Assertions.assertThat;

class SeatAllocatorTest {

    private static final Instant NOW = Instant.parse("2026-03-02T06:00:00Z");

    private static List<SeatBooking> bookings(int count) {
        return IntStream.range(0, count).mapToObj(i -> new SeatBooking(null, null, NOW.minusSeconds(100 - i))).toList();
    }

    @Test
    void everyoneWaitsUntilThereIsADriver() {
        List<SeatBooking> active = bookings(3);

        List<SeatBooking> confirmed = SeatAllocator.allocate(false, 0, active, NOW);

        assertThat(confirmed).isEmpty();
        assertThat(active).extracting(SeatBooking::getStatus).containsOnly(PENDING);
    }

    @Test
    void firstRequestsFillTheCar() {
        List<SeatBooking> active = bookings(4);

        List<SeatBooking> confirmed = SeatAllocator.allocate(true, 2, active, NOW);

        assertThat(confirmed).containsExactly(active.get(0), active.get(1));
        assertThat(active).extracting(SeatBooking::getStatus).containsExactly(CONFIRMED, CONFIRMED, WAITLISTED, WAITLISTED);
    }

    @Test
    void onlyReportsNewlyConfirmedBookings() {
        List<SeatBooking> active = bookings(3);
        SeatAllocator.allocate(true, 2, active, NOW);

        List<SeatBooking> confirmed = SeatAllocator.allocate(true, 3, active, NOW);

        assertThat(confirmed).containsExactly(active.get(2));
    }

    @Test
    void losingTheDriverPutsEveryoneBackToPending() {
        List<SeatBooking> active = bookings(2);
        SeatAllocator.allocate(true, 4, active, NOW);

        SeatAllocator.allocate(false, 0, active, NOW);

        assertThat(active).extracting(SeatBooking::getStatus).containsOnly(PENDING);
    }

    @Test
    void fewerSeatsMovesTheLatestToTheWaitlist() {
        List<SeatBooking> active = bookings(3);
        SeatAllocator.allocate(true, 3, active, NOW);

        SeatAllocator.allocate(true, 1, active, NOW);

        assertThat(active).extracting(SeatBooking::getStatus).containsExactly(CONFIRMED, WAITLISTED, WAITLISTED);
    }

    @Test
    void statusChangesTouchUpdatedAt() {
        List<SeatBooking> active = bookings(1);
        Instant later = NOW.plusSeconds(60);

        SeatAllocator.allocate(true, 1, active, later);

        assertThat(active.get(0).getUpdatedAt()).isEqualTo(later);
    }
}
