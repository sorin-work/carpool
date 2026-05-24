package com.carpool.rotation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Picks a driver for each ride so that driving is shared as evenly as possible. For every ride, in
 * departure order, the eligible member who has driven the fewest times takes it; ties go to whoever
 * has gone longest without driving, then to the longest-standing member. Where possible nobody drives
 * twice on the same day.
 */
public final class FairRotation {

    /** A ride that needs a driver and how many seats its riders need. */
    public record Slot(Long rideId, LocalDate date, int seatsNeeded) {
    }

    /**
     * A member who can drive. {@code drives} and {@code lastDrove} describe the recent history;
     * {@code away} lists the dates the member is not available.
     */
    public record Candidate(Long memberId, int carSeats, int drives, LocalDate lastDrove, Set<LocalDate> away) {
    }

    private FairRotation() {
    }

    /** Returns ride id -> member id, in slot order. Rides nobody can take map to {@code null}. */
    public static Map<Long, Long> assign(List<Slot> slots, List<Candidate> candidates) {
        List<Tally> tallies = new ArrayList<>();
        for (Candidate candidate : candidates) {
            tallies.add(new Tally(candidate));
        }
        Comparator<Tally> fairest = Comparator.comparingInt((Tally t) -> t.drives)
                .thenComparing(t -> t.lastDrove, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(t -> t.candidate.memberId());

        Map<Long, Long> result = new LinkedHashMap<>();
        for (Slot slot : slots) {
            // Preferences, strongest first: room for every rider, then not driving twice that day.
            // A smaller car or a second run is still better than no driver at all.
            Optional<Tally> pick = pick(slot, tallies, fairest, true, true);
            if (pick.isEmpty()) {
                pick = pick(slot, tallies, fairest, true, false);
            }
            if (pick.isEmpty()) {
                pick = pick(slot, tallies, fairest, false, true);
            }
            if (pick.isEmpty()) {
                pick = pick(slot, tallies, fairest, false, false);
            }
            result.put(slot.rideId(), pick.map(t -> {
                t.drives++;
                t.lastDrove = slot.date();
                return t.candidate.memberId();
            }).orElse(null));
        }
        return result;
    }

    private static Optional<Tally> pick(Slot slot, List<Tally> tallies, Comparator<Tally> order, boolean needRoom,
                                        boolean onePerDay) {
        return tallies.stream()
                .filter(t -> !t.candidate.away().contains(slot.date()))
                .filter(t -> t.candidate.carSeats() > 0)
                .filter(t -> !needRoom || t.candidate.carSeats() >= slot.seatsNeeded())
                .filter(t -> !onePerDay || !slot.date().equals(t.lastDrove))
                .min(order);
    }

    private static final class Tally {
        final Candidate candidate;
        int drives;
        LocalDate lastDrove;

        Tally(Candidate candidate) {
            this.candidate = candidate;
            this.drives = candidate.drives();
            this.lastDrove = candidate.lastDrove();
        }
    }
}
