package com.carpool.ride;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Wording shared by every notification about a ride. */
public final class RideMessages {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("EEE d MMM 'at' HH:mm", Locale.ENGLISH);

    private RideMessages() {
    }

    /** e.g. {@code **Morning school run** on Tue 3 Mar at 07:45} */
    public static String describe(Ride ride) {
        ZonedDateTime local = ride.getDepartureAt().atZone(ride.getTrip().getGroup().getZone());
        return "**" + ride.getTrip().getName() + "** on " + WHEN.format(local);
    }

    public static String link(Ride ride) {
        return "/groups/" + ride.getTrip().getGroup().getId() + "/rides/" + ride.getId();
    }

    public static String bold(String text) {
        return "**" + text + "**";
    }
}
