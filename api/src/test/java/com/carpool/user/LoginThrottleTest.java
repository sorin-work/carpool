package com.carpool.user;

import com.carpool.support.MutableClock;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class LoginThrottleTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-07-01T10:00:00Z"));
    private final LoginThrottle throttle = new LoginThrottle(clock);

    @Test
    void blocksAfterFiveFailures() {
        for (int i = 0; i < 4; i++) {
            throttle.failed("a@example.com");
        }
        assertThat(throttle.isBlocked("a@example.com")).isFalse();

        throttle.failed("a@example.com");

        assertThat(throttle.isBlocked("a@example.com")).isTrue();
        assertThat(throttle.isBlocked("b@example.com")).isFalse();
    }

    @Test
    void unblocksWhenTheWindowHasPassed() {
        for (int i = 0; i < 5; i++) {
            throttle.failed("a@example.com");
        }
        clock.advance(Duration.ofMinutes(16));

        assertThat(throttle.isBlocked("a@example.com")).isFalse();
    }

    @Test
    void aSuccessfulLoginResetsTheCount() {
        for (int i = 0; i < 4; i++) {
            throttle.failed("a@example.com");
        }
        throttle.succeeded("a@example.com");
        throttle.failed("a@example.com");

        assertThat(throttle.isBlocked("a@example.com")).isFalse();
    }
}
