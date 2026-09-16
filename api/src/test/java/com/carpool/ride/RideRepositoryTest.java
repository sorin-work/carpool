package com.carpool.ride;

import com.carpool.support.Fixtures;
import com.carpool.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RideRepositoryTest extends IntegrationTest {

    @Autowired
    private RideRepository rides;

    private long groupId;
    private JsonNode week;
    private String ana;

    @BeforeEach
    void setUp() throws Exception {
        ana = register("ana@example.com", "Ana");
        groupId = createGroup(ana, "School run").get("id").asLong();
        mvc.perform(postAs(ana, "/api/groups/{id}/trips", groupId).content(toJson(Fixtures.schoolRun())));
        mvc.perform(putAs(ana, "/api/groups/{id}/members/me", groupId).content(toJson(Fixtures.car(3))));
        week = read(mvc.perform(getAs(ana, "/api/groups/{id}/rides", groupId)
                .param("from", "2026-03-03").param("to", "2026-03-07")).andReturn());
        for (int i = 0; i < 3; i++) {
            mvc.perform(postAs(ana, "/api/rides/{id}/driver", week.get(i).get("id").asLong()));
        }
        mvc.perform(postAs(ana, "/api/rides/{id}/cancel", week.get(2).get("id").asLong()));
    }

    @Test
    void completedRidesLeftAndWereNotCancelled() {
        List<Ride> done = rides.findCompleted(groupId, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 8),
                Instant.parse("2026-03-05T06:00:00Z"));

        assertThat(done).extracting(Ride::getRideDate)
                .containsExactly(LocalDate.of(2026, 3, 3), LocalDate.of(2026, 3, 4));
    }

    @Test
    void driverHistoryIgnoresCancelledRides() {
        List<DriverHistory> history = rides.findDriverHistory(groupId, LocalDate.of(2026, 3, 1));

        assertThat(history).singleElement().satisfies(h -> {
            assertThat(h.drives()).isEqualTo(2);
            assertThat(h.lastDrove()).isEqualTo(LocalDate.of(2026, 3, 4));
        });
    }

    @Test
    void cancelledRidesAreCountedOnceTheyWouldHaveLeft() {
        assertThat(rides.countCancelled(groupId, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 8),
                Instant.parse("2026-03-04T12:00:00Z"))).isZero();
        assertThat(rides.countCancelled(groupId, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 8),
                Instant.parse("2026-03-06T12:00:00Z"))).isEqualTo(1);
    }

    @Test
    void datesForATripUseAnExclusiveEnd() {
        long tripId = week.get(0).get("tripId").asLong();

        assertThat(rides.findDatesForTrip(tripId, LocalDate.of(2026, 3, 3), LocalDate.of(2026, 3, 5)))
                .containsExactlyInAnyOrder(LocalDate.of(2026, 3, 3), LocalDate.of(2026, 3, 4));
    }
}
