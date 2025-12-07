package com.carpool.trip;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.List;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.assertj.core.api.Assertions.assertThat;

class DaysOfWeekTest {

    @Test
    void mondayIsTheLowestBit() {
        assertThat(DaysOfWeek.toMask(List.of(MONDAY))).isEqualTo(1);
        assertThat(DaysOfWeek.toMask(List.of(SUNDAY))).isEqualTo(64);
    }

    @Test
    void weekdaysConstantMatchesMondayToFriday() {
        assertThat(DaysOfWeek.fromMask(DaysOfWeek.WEEKDAYS))
                .containsExactlyInAnyOrderElementsOf(EnumSet.range(MONDAY, FRIDAY));
    }

    @Test
    void roundTripsAnySet() {
        for (int mask = 0; mask < 128; mask++) {
            assertThat(DaysOfWeek.toMask(DaysOfWeek.fromMask(mask))).isEqualTo(mask);
        }
    }

    @Test
    void duplicatesDoNotChangeTheMask() {
        assertThat(DaysOfWeek.toMask(List.of(WEDNESDAY, WEDNESDAY))).isEqualTo(DaysOfWeek.toMask(List.of(WEDNESDAY)));
    }

    @Test
    void includesChecksASingleDay() {
        int mask = DaysOfWeek.toMask(List.of(MONDAY, WEDNESDAY, FRIDAY));
        assertThat(DaysOfWeek.includes(mask, WEDNESDAY)).isTrue();
        assertThat(DaysOfWeek.includes(mask, SATURDAY)).isFalse();
        assertThat(EnumSet.allOf(DayOfWeek.class).stream().filter(d -> DaysOfWeek.includes(mask, d))).hasSize(3);
    }
}
