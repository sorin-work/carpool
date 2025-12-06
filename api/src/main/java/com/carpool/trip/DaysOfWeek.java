package com.carpool.trip;

import java.time.DayOfWeek;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/** Packs a set of weekdays into the integer stored in {@code trips.days_of_week}. */
public final class DaysOfWeek {

    public static final int WEEKDAYS = 0b0011111;

    private DaysOfWeek() {
    }

    public static int toMask(Collection<DayOfWeek> days) {
        int mask = 0;
        for (DayOfWeek day : days) {
            mask |= bit(day);
        }
        return mask;
    }

    public static Set<DayOfWeek> fromMask(int mask) {
        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            if ((mask & bit(day)) != 0) {
                days.add(day);
            }
        }
        return days;
    }

    public static boolean includes(int mask, DayOfWeek day) {
        return (mask & bit(day)) != 0;
    }

    private static int bit(DayOfWeek day) {
        return 1 << (day.getValue() - 1);
    }
}
