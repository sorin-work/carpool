package com.carpool.security;

import com.carpool.config.JwtProperties;
import com.carpool.support.MutableClock;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-that-is-long-enough-0123456789";

    private final MutableClock clock = new MutableClock(Instant.parse("2026-01-10T10:00:00Z"));
    private final JwtService service = new JwtService(new JwtProperties(SECRET, Duration.ofHours(1)), clock);

    @Test
    void roundTripsTheUser() {
        String token = service.issue(new AuthUser(42L, "ana@example.com"));

        assertThat(service.verify(token)).contains(new AuthUser(42L, "ana@example.com"));
        assertThat(service.expiresAt(token)).isEqualTo(Instant.parse("2026-01-10T11:00:00Z"));
    }

    @Test
    void rejectsExpiredTokens() {
        String token = service.issue(new AuthUser(1L, "a@example.com"));
        clock.advance(Duration.ofMinutes(61));

        assertThat(service.verify(token)).isEmpty();
    }

    @Test
    void rejectsTokensSignedWithAnotherKey() {
        JwtService other = new JwtService(new JwtProperties("another-secret-that-is-long-enough-xyz", null), clock);
        String token = other.issue(new AuthUser(1L, "a@example.com"));

        assertThat(service.verify(token)).isEmpty();
    }

    @Test
    void rejectsGarbage() {
        assertThat(service.verify("not-a-token")).isEmpty();
        assertThat(service.verify("")).isEmpty();
    }

    @Test
    void requiresALongEnoughSecret() {
        assertThatThrownBy(() -> new JwtProperties("short", null)).isInstanceOf(IllegalArgumentException.class);
    }
}
