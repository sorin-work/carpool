package com.carpool.user;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Slows down password guessing: after {@value #MAX_FAILURES} failed logins for an email within
 * {@link #WINDOW}, further attempts are refused until the window has passed. Kept in memory, which
 * is enough for a single API instance.
 */
@Component
public class LoginThrottle {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);

    private record Failures(int count, Instant since) {
    }

    private final ConcurrentHashMap<String, Failures> failures = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginThrottle(Clock clock) {
        this.clock = clock;
    }

    public boolean isBlocked(String email) {
        Failures f = failures.get(email);
        if (f == null) {
            return false;
        }
        if (f.since().plus(WINDOW).isBefore(clock.instant())) {
            failures.remove(email, f);
            return false;
        }
        return f.count() >= MAX_FAILURES;
    }

    public void failed(String email) {
        Instant now = clock.instant();
        failures.compute(email, (key, f) -> f == null || f.since().plus(WINDOW).isBefore(now)
                ? new Failures(1, now)
                : new Failures(f.count() + 1, f.since()));
    }

    public void succeeded(String email) {
        failures.remove(email);
    }
}
