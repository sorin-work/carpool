package com.carpool.ride;

import com.carpool.common.ApiException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RideRulesTest {

    private static final Instant DEPARTURE = Instant.parse("2026-03-03T05:45:00Z");

    private static Ride ride() {
        return new Ride(null, LocalDate.of(2026, 3, 3), DEPARTURE, Instant.EPOCH);
    }

    @Test
    void openBeforeDeparture() {
        assertThatCode(() -> RideRules.requireOpen(ride(), DEPARTURE.minusSeconds(60))).doesNotThrowAnyException();
    }

    @Test
    void closedFromTheMomentItLeaves() {
        assertThatThrownBy(() -> RideRules.requireOpen(ride(), DEPARTURE))
                .isInstanceOf(ApiException.class)
                .hasMessage("This ride has already left");
    }

    @Test
    void closedWhenCancelled() {
        Ride ride = ride();
        ride.cancel("Snow");

        assertThatThrownBy(() -> RideRules.requireOpen(ride, DEPARTURE.minusSeconds(3600)))
                .hasMessage("This ride was cancelled");
    }
}
