package com.carpool.support;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Fixtures {

    private Fixtures() {
    }

    /** Mon-Fri 07:45, 6.4 km, from Monday 2 March 2026. */
    public static Map<String, Object> schoolRun() {
        Map<String, Object> trip = new HashMap<>();
        trip.put("name", "Morning school run");
        trip.put("origin", "12 Elm St");
        trip.put("destination", "Riverside Primary");
        trip.put("departureTime", "07:45");
        trip.put("days", List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"));
        trip.put("distanceKm", 6.4);
        trip.put("startsOn", "2026-03-02");
        return trip;
    }

    public static Map<String, Object> car(int seats) {
        Map<String, Object> body = new HashMap<>();
        body.put("canDrive", true);
        body.put("carSeats", seats);
        return body;
    }
}
