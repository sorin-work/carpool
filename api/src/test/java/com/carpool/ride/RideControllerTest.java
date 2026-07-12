package com.carpool.ride;

import com.carpool.support.Fixtures;
import com.carpool.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RideControllerTest extends IntegrationTest {

    private String ana;
    private String ben;
    private long groupId;
    private long tripId;

    @BeforeEach
    void setUp() throws Exception {
        ana = register("ana@example.com", "Ana");
        ben = register("ben@example.com", "Ben");
        JsonNode group = createGroup(ana, "School run");
        groupId = group.get("id").asLong();
        joinGroup(ben, group.get("inviteCode").asText());
        tripId = read(mvc.perform(postAs(ana, "/api/groups/{id}/trips", groupId).content(toJson(Fixtures.schoolRun())))
                .andReturn()).get("id").asLong();
        mvc.perform(putAs(ana, "/api/groups/{id}/members/me", groupId).content(toJson(Fixtures.car(3))));
    }

    private JsonNode schedule(String token, String from, String to) throws Exception {
        return read(mvc.perform(getAs(token, "/api/groups/{id}/rides", groupId).param("from", from).param("to", to))
                .andExpect(status().isOk()).andReturn());
    }

    @Test
    void listsTheRidesInTheRange() throws Exception {
        mvc.perform(getAs(ben, "/api/groups/{id}/rides", groupId).param("from", "2026-03-02").param("to", "2026-03-09"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].date").value("2026-03-02"))
                .andExpect(jsonPath("$[0].departureAt").value("2026-03-02T05:45:00Z"))
                .andExpect(jsonPath("$[0].tripName").value("Morning school run"))
                .andExpect(jsonPath("$[0].status").value("SCHEDULED"));
    }

    @Test
    void readingTheScheduleTwiceDoesNotDuplicateRides() throws Exception {
        JsonNode first = schedule(ana, "2026-03-02", "2026-03-09");
        JsonNode second = schedule(ana, "2026-03-02", "2026-03-16");

        assertIds(first, second);
        assertThat(second.size()).isEqualTo(10);
    }

    private static void assertIds(JsonNode first, JsonNode second) {
        for (int i = 0; i < first.size(); i++) {
            assertThat(second.get(i).get("id").asLong())
                    .isEqualTo(first.get(i).get("id").asLong());
        }
    }

    @Test
    void defaultsToTwoWeeksFromToday() throws Exception {
        mvc.perform(getAs(ana, "/api/groups/{id}/rides", groupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)));
    }

    @Test
    void limitsTheRange() throws Exception {
        mvc.perform(getAs(ana, "/api/groups/{id}/rides", groupId).param("from", "2026-03-01").param("to", "2026-06-01"))
                .andExpect(status().isBadRequest());
        mvc.perform(getAs(ana, "/api/groups/{id}/rides", groupId).param("from", "2026-03-05").param("to", "2026-03-05"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pausedTripsDropOutOfTheUpcomingSchedule() throws Exception {
        schedule(ana, "2026-03-02", "2026-03-09");
        mvc.perform(postAs(ana, "/api/trips/{id}/pause", tripId));

        // Monday's ride already left, so it stays in the history.
        JsonNode rides = schedule(ana, "2026-03-02", "2026-03-09");
        assertThat(rides.size()).isEqualTo(1);
    }

    @Test
    void aDriverTakesARide() throws Exception {
        long rideId = schedule(ana, "2026-03-03", "2026-03-04").get(0).get("id").asLong();

        mvc.perform(postAs(ana, "/api/rides/{id}/driver", rideId).content(toJson(Map.of("seats", 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driver.displayName").value("Ana"))
                .andExpect(jsonPath("$.seatsOffered").value(2));
    }

    @Test
    void seatsDefaultToTheWholeCar() throws Exception {
        long rideId = schedule(ana, "2026-03-03", "2026-03-04").get(0).get("id").asLong();

        mvc.perform(postAs(ana, "/api/rides/{id}/driver", rideId))
                .andExpect(jsonPath("$.seatsOffered").value(3));
    }

    @Test
    void membersWithoutACarCannotDrive() throws Exception {
        long rideId = schedule(ana, "2026-03-03", "2026-03-04").get(0).get("id").asLong();

        mvc.perform(postAs(ben, "/api/rides/{id}/driver", rideId)).andExpect(status().isBadRequest());
    }

    @Test
    void onlyOneDriverPerRide() throws Exception {
        long rideId = schedule(ana, "2026-03-03", "2026-03-04").get(0).get("id").asLong();
        mvc.perform(putAs(ben, "/api/groups/{id}/members/me", groupId).content(toJson(Fixtures.car(4))));
        mvc.perform(postAs(ana, "/api/rides/{id}/driver", rideId));

        mvc.perform(postAs(ben, "/api/rides/{id}/driver", rideId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ana is already driving"));
    }

    @Test
    void cannotTakeARideThatLeft() throws Exception {
        long rideId = schedule(ana, "2026-03-02", "2026-03-03").get(0).get("id").asLong();

        mvc.perform(postAs(ana, "/api/rides/{id}/driver", rideId)).andExpect(status().isConflict());
    }

    @Test
    void driversStepDown() throws Exception {
        long rideId = schedule(ana, "2026-03-03", "2026-03-04").get(0).get("id").asLong();
        mvc.perform(postAs(ana, "/api/rides/{id}/driver", rideId));

        mvc.perform(deleteAs(ana, "/api/rides/{id}/driver", rideId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driver").doesNotExist())
                .andExpect(jsonPath("$.seatsOffered").value(0));
    }

    @Test
    void theDriverLeavesANote() throws Exception {
        long rideId = schedule(ana, "2026-03-03", "2026-03-04").get(0).get("id").asLong();
        mvc.perform(postAs(ana, "/api/rides/{id}/driver", rideId));

        mvc.perform(putAs(ana, "/api/rides/{id}/note", rideId).content(toJson(Map.of("note", " Back gate today "))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.note").value("Back gate today"));
        mvc.perform(getAs(ben, "/api/rides/{id}", rideId)).andExpect(jsonPath("$.note").value("Back gate today"));
    }

    @Test
    void ridersCannotChangeTheNote() throws Exception {
        long rideId = schedule(ana, "2026-03-03", "2026-03-04").get(0).get("id").asLong();

        mvc.perform(putAs(ben, "/api/rides/{id}/note", rideId).content(toJson(Map.of("note", "Free candy"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminsAssignADriver() throws Exception {
        long rideId = schedule(ana, "2026-03-03", "2026-03-04").get(0).get("id").asLong();
        long benId = read(mvc.perform(putAs(ben, "/api/groups/{id}/members/me", groupId).content(toJson(Fixtures.car(2))))
                .andReturn()).get("id").asLong();

        mvc.perform(putAs(ana, "/api/rides/{r}/driver/{m}", rideId, benId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driver.displayName").value("Ben"))
                .andExpect(jsonPath("$.seatsOffered").value(2));
        mvc.perform(getAs(ben, "/api/notifications"))
                .andExpect(jsonPath("$.items[0].type").value("DRIVER_ASSIGNED"));
    }

    @Test
    void membersCannotAssignDrivers() throws Exception {
        long rideId = schedule(ana, "2026-03-03", "2026-03-04").get(0).get("id").asLong();
        long anaId = read(mvc.perform(getAs(ana, "/api/groups/{id}", groupId)).andReturn()).get("myMemberId").asLong();

        mvc.perform(putAs(ben, "/api/rides/{r}/driver/{m}", rideId, anaId)).andExpect(status().isForbidden());
    }

    @Test
    void assignedDriversNeedACar() throws Exception {
        long rideId = schedule(ana, "2026-03-03", "2026-03-04").get(0).get("id").asLong();
        long benId = read(mvc.perform(getAs(ben, "/api/groups/{id}", groupId)).andReturn()).get("myMemberId").asLong();

        mvc.perform(putAs(ana, "/api/rides/{r}/driver/{m}", rideId, benId)).andExpect(status().isBadRequest());
    }

    @Test
    void editingATripMovesItsPlannedRides() throws Exception {
        schedule(ana, "2026-03-03", "2026-03-10");
        Map<String, Object> changed = Fixtures.schoolRun();
        changed.put("departureTime", "08:10");
        changed.put("days", java.util.List.of("MONDAY", "WEDNESDAY", "FRIDAY"));

        mvc.perform(putAs(ana, "/api/trips/{id}", tripId).content(toJson(changed))).andExpect(status().isOk());

        JsonNode rides = schedule(ana, "2026-03-03", "2026-03-10");
        java.util.List<JsonNode> running = new java.util.ArrayList<>();
        rides.forEach(r -> {
            if (r.get("status").asText().equals("SCHEDULED")) {
                running.add(r);
            }
        });
        assertThat(running).hasSize(3);
        assertThat(running.get(0).get("date").asText()).isEqualTo("2026-03-04");
        assertThat(running.get(0).get("departureAt").asText()).isEqualTo("2026-03-04T06:10:00Z");
    }

    @Test
    void ridersOfDroppedDatesAreTold() throws Exception {
        long tuesday = schedule(ana, "2026-03-03", "2026-03-04").get(0).get("id").asLong();
        mvc.perform(postAs(ben, "/api/rides/{id}/seat", tuesday));
        Map<String, Object> changed = Fixtures.schoolRun();
        changed.put("days", java.util.List.of("MONDAY"));

        mvc.perform(putAs(ana, "/api/trips/{id}", tripId).content(toJson(changed)));

        mvc.perform(getAs(ben, "/api/rides/{id}", tuesday)).andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(getAs(ben, "/api/notifications")).andExpect(jsonPath("$.items[0].type").value("RIDE_CANCELLED"));
    }
}
