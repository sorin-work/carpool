package com.carpool.rotation;

import com.carpool.rotation.FairRotation.Candidate;
import com.carpool.rotation.FairRotation.Slot;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class FairRotationTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 3, 2);

    private static List<Slot> weekdays(int count, int seatsNeeded) {
        List<Slot> slots = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            slots.add(new Slot((long) i + 100, MONDAY.plusDays(i), seatsNeeded));
        }
        return slots;
    }

    private static Candidate fresh(long id, int seats) {
        return new Candidate(id, seats, 0, null, Set.of());
    }

    private static Map<Long, Long> counts(Collection<Long> drivers) {
        return drivers.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    @Test
    void sharesDrivingEvenly() {
        Map<Long, Long> result = FairRotation.assign(weekdays(12, 2),
                List.of(fresh(1, 3), fresh(2, 3), fresh(3, 4)));

        assertThat(counts(result.values())).containsOnly(Map.entry(1L, 4L), Map.entry(2L, 4L), Map.entry(3L, 4L));
    }

    @Test
    void whoeverDroveLeastGoesFirst() {
        Map<Long, Long> result = FairRotation.assign(weekdays(2, 1), List.of(
                new Candidate(1L, 3, 5, MONDAY.minusDays(7), Set.of()),
                new Candidate(2L, 3, 2, MONDAY.minusDays(3), Set.of())));

        assertThat(result.values()).containsExactly(2L, 2L);
    }

    @Test
    void someoneWhoNeverDroveWinsATie() {
        Map<Long, Long> result = FairRotation.assign(weekdays(1, 1), List.of(
                new Candidate(1L, 3, 0, MONDAY.minusDays(14), Set.of()),
                new Candidate(2L, 3, 0, null, Set.of())));

        assertThat(result.values()).containsExactly(2L);
    }

    @Test
    void onATieWhoeverDroveLongestAgoGoes() {
        Map<Long, Long> result = FairRotation.assign(weekdays(1, 1), List.of(
                new Candidate(1L, 3, 4, MONDAY.minusDays(3), Set.of()),
                new Candidate(2L, 3, 4, MONDAY.minusDays(10), Set.of())));

        assertThat(result.values()).containsExactly(2L);
    }

    @Test
    void twoFreshDriversAlternate() {
        Map<Long, Long> result = FairRotation.assign(weekdays(4, 1), List.of(fresh(1, 3), fresh(2, 3)));

        assertThat(result.values()).containsExactly(1L, 2L, 1L, 2L);
    }

    @Test
    void equalHistoryFallsBackToMemberOrder() {
        Map<Long, Long> result = FairRotation.assign(weekdays(1, 1), List.of(fresh(7, 2), fresh(3, 2)));

        assertThat(result.values()).containsExactly(3L);
    }

    @Test
    void skipsMembersWhoAreAway() {
        Map<Long, Long> result = FairRotation.assign(weekdays(1, 1), List.of(
                new Candidate(1L, 3, 0, null, Set.of(MONDAY)),
                new Candidate(2L, 3, 9, null, Set.of())));

        assertThat(result.values()).containsExactly(2L);
    }

    @Test
    void prefersACarWithRoomForEveryRider() {
        Map<Long, Long> result = FairRotation.assign(weekdays(1, 3), List.of(fresh(1, 2), new Candidate(2L, 4, 3, null, Set.of())));

        assertThat(result.values()).containsExactly(2L);
    }

    @Test
    void takesASmallerCarWhenNothingBiggerIsFree() {
        Map<Long, Long> result = FairRotation.assign(weekdays(1, 5), List.of(fresh(1, 2), fresh(2, 3)));

        assertThat(result.values()).containsExactly(1L);
    }

    @Test
    void leavesARideOpenWhenNobodyCanDrive() {
        Map<Long, Long> result = FairRotation.assign(weekdays(1, 1), List.of(new Candidate(1L, 3, 0, null, Set.of(MONDAY))));

        assertThat(result).containsEntry(100L, null);
    }

    @Test
    void noCandidatesMeansNoDrivers() {
        assertThat(FairRotation.assign(weekdays(3, 1), List.of()).values()).containsOnlyNulls();
    }
}
