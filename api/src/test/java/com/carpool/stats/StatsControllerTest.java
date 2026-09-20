package com.carpool.stats;

import com.carpool.support.Fixtures;
import com.carpool.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StatsControllerTest extends IntegrationTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private String ana;
    private String ben;
    private String cara;
    private long groupId;
    private JsonNode week;

    @BeforeEach
    void setUp() throws Exception {
        ana = register("ana@example.com", "Ana");
        ben = register("ben@example.com", "Ben");
        cara = register("cara@example.com", "Cara");
        JsonNode group = createGroup(ana, "School run");
        groupId = group.get("id").asLong();
        joinGroup(ben, group.get("inviteCode").asText());
        joinGroup(cara, group.get("inviteCode").asText());
        mvc.perform(postAs(ana, "/api/groups/{id}/trips", groupId).content(toJson(Fixtures.schoolRun())));
        mvc.perform(putAs(ana, "/api/groups/{id}/members/me", groupId).content(toJson(Fixtures.car(3))));
        mvc.perform(putAs(ben, "/api/groups/{id}/members/me", groupId).content(toJson(Fixtures.car(3))));
        // Tue-Fri: Ana drives Tue and Wed, Ben drives Thu, Friday is cancelled. Cara rides every day.
        week = read(mvc.perform(getAs(ana, "/api/groups/{id}/rides", groupId)
                .param("from", "2026-03-03").param("to", "2026-03-07")).andReturn());
        drive(ana, 0);
        drive(ana, 1);
        drive(ben, 2);
        drive(ben, 3);
        for (int i = 0; i < 4; i++) {
            mvc.perform(postAs(cara, "/api/rides/{id}/seat", rideId(i)));
        }
        mvc.perform(postAs(ben, "/api/rides/{id}/seat", rideId(0)));
        mvc.perform(postAs(ben, "/api/rides/{id}/cancel", rideId(3)));
        clock.set(Instant.parse("2026-03-07T10:00:00Z"));
    }

    private long rideId(int day) {
        return week.get(day).get("id").asLong();
    }

    private void drive(String token, int day) throws Exception {
        mvc.perform(postAs(token, "/api/rides/{id}/driver", rideId(day)));
    }

    @Test
    void countsDrivesAndRides() throws Exception {
        mvc.perform(getAs(cara, "/api/groups/{id}/stats", groupId).param("from", "2026-03-01").param("to", "2026-03-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rides").value(3))
                .andExpect(jsonPath("$.cancelled").value(1))
                .andExpect(jsonPath("$.members[0].displayName").value("Ana"))
                .andExpect(jsonPath("$.members[0].drives").value(2))
                .andExpect(jsonPath("$.members[0].kmDriven").value(12.8))
                .andExpect(jsonPath("$.members[0].passengersCarried").value(3))
                .andExpect(jsonPath("$.members[0].lastDrove").value("2026-03-04"))
                .andExpect(jsonPath("$.members[1].displayName").value("Ben"))
                .andExpect(jsonPath("$.members[1].drives").value(1))
                .andExpect(jsonPath("$.members[1].ridesTaken").value(1))
                .andExpect(jsonPath("$.members[1].driveShare").value(50))
                .andExpect(jsonPath("$.members[2].displayName").value("Cara"))
                .andExpect(jsonPath("$.members[2].ridesTaken").value(3))
                .andExpect(jsonPath("$.members[2].kmRidden").value(19.2))
                .andExpect(jsonPath("$.members[2].driveShare").value(0));
    }

    @Test
    void ridesThatHaveNotLeftDoNotCount() throws Exception {
        clock.set(Instant.parse("2026-03-04T05:00:00Z"));

        mvc.perform(getAs(cara, "/api/groups/{id}/stats", groupId).param("from", "2026-03-01").param("to", "2026-03-08"))
                .andExpect(jsonPath("$.rides").value(1))
                .andExpect(jsonPath("$.members[0].drives").value(1));
    }

    @Test
    void defaultsToTheLastNinetyDays() throws Exception {
        mvc.perform(getAs(cara, "/api/groups/{id}/stats", groupId))
                .andExpect(jsonPath("$.to").value("2026-03-08"))
                .andExpect(jsonPath("$.from").value("2025-12-08"))
                .andExpect(jsonPath("$.rides").value(3));
    }

    @Test
    void countsDrivesThatAreStillAhead() throws Exception {
        clock.set(Instant.parse("2026-03-02T06:00:00Z"));

        mvc.perform(getAs(cara, "/api/groups/{id}/stats", groupId).param("from", "2026-02-01").param("to", "2026-03-02"))
                .andExpect(jsonPath("$.members[0].displayName").value("Ana"))
                .andExpect(jsonPath("$.members[0].upcomingDrives").value(2))
                // Friday's ride is cancelled, so Ben only has Thursday.
                .andExpect(jsonPath("$.members[1].upcomingDrives").value(1));
    }

    @Test
    void listsOneMembersRides() throws Exception {
        long benId = read(mvc.perform(getAs(ben, "/api/groups/{id}", groupId)).andReturn()).get("myMemberId").asLong();

        mvc.perform(getAs(cara, "/api/groups/{g}/members/{m}/rides", groupId, benId)
                        .param("from", "2026-03-01").param("to", "2026-03-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].date").value("2026-03-05"))
                .andExpect(jsonPath("$[0].driver").value(true))
                .andExpect(jsonPath("$[1].date").value("2026-03-03"))
                .andExpect(jsonPath("$[1].driver").value(false));
    }

    @Test
    void outsidersGetNothing() throws Exception {
        String eve = register("eve@example.com", "Eve");

        mvc.perform(getAs(eve, "/api/groups/{id}/stats", groupId)).andExpect(status().isNotFound());
    }

    @Test
    void queryCountDoesNotGrowWithTheNumberOfRides() throws Exception {
        clock.set(Instant.parse("2026-03-02T06:00:00Z"));
        for (String time : new String[]{"12:30", "15:30"}) {
            Map<String, Object> trip = Fixtures.schoolRun();
            trip.put("name", "Run at " + time);
            trip.put("departureTime", time);
            mvc.perform(postAs(ana, "/api/groups/{id}/trips", groupId).content(toJson(trip)));
        }
        JsonNode all = read(mvc.perform(getAs(ana, "/api/groups/{id}/rides", groupId)
                .param("from", "2026-03-03").param("to", "2026-03-07")).andReturn());
        for (JsonNode ride : all) {
            if (!ride.has("driver")) {
                mvc.perform(postAs(ana, "/api/rides/{id}/driver", ride.get("id").asLong()));
                mvc.perform(postAs(cara, "/api/rides/{id}/seat", ride.get("id").asLong()));
            }
        }
        clock.set(Instant.parse("2026-03-07T10:00:00Z"));
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        mvc.perform(getAs(cara, "/api/groups/{id}/stats", groupId).param("from", "2026-03-01").param("to", "2026-03-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rides").value(11));

        assertThat(statistics.getPrepareStatementCount()).isLessThan(8);
    }
}
