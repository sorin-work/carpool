package com.carpool.trip;

import com.carpool.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TripControllerTest extends IntegrationTest {

    private String ana;
    private String ben;
    private long groupId;

    @BeforeEach
    void setUp() throws Exception {
        ana = register("ana@example.com", "Ana");
        ben = register("ben@example.com", "Ben");
        JsonNode group = createGroup(ana, "School run");
        groupId = group.get("id").asLong();
        joinGroup(ben, group.get("inviteCode").asText());
    }

    static Map<String, Object> schoolRun() {
        Map<String, Object> trip = new HashMap<>();
        trip.put("name", "Morning school run");
        trip.put("origin", "12 Elm St");
        trip.put("destination", "Riverside Primary");
        trip.put("departureTime", "07:45");
        trip.put("days", List.of("FRIDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY"));
        trip.put("distanceKm", 6.4);
        trip.put("startsOn", "2026-03-02");
        return trip;
    }

    @Test
    void adminsCreateTrips() throws Exception {
        mvc.perform(postAs(ana, "/api/groups/{id}/trips", groupId).content(toJson(schoolRun())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Morning school run"))
                .andExpect(jsonPath("$.departureTime").value("07:45:00"))
                .andExpect(jsonPath("$.days", hasSize(5)))
                .andExpect(jsonPath("$.days[0]").value("MONDAY"))
                .andExpect(jsonPath("$.distanceKm").value(6.4))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void membersSeeTripsButCannotCreateThem() throws Exception {
        mvc.perform(postAs(ana, "/api/groups/{id}/trips", groupId).content(toJson(schoolRun())));

        mvc.perform(getAs(ben, "/api/groups/{id}/trips", groupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(postAs(ben, "/api/groups/{id}/trips", groupId).content(toJson(schoolRun())))
                .andExpect(status().isForbidden());
    }

    @Test
    void requiresAtLeastOneDay() throws Exception {
        Map<String, Object> trip = schoolRun();
        trip.put("days", List.of());

        mvc.perform(postAs(ana, "/api/groups/{id}/trips", groupId).content(toJson(trip)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.days").exists());
    }

    @Test
    void rejectsAZeroOrNegativeDistance() throws Exception {
        for (double km : new double[]{0, -4.5}) {
            Map<String, Object> trip = schoolRun();
            trip.put("distanceKm", km);

            mvc.perform(postAs(ana, "/api/groups/{id}/trips", groupId).content(toJson(trip)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fields.distanceKm").exists());
        }
    }

    @Test
    void rejectsAnEndBeforeTheStart() throws Exception {
        Map<String, Object> trip = schoolRun();
        trip.put("endsOn", "2026-03-01");

        mvc.perform(postAs(ana, "/api/groups/{id}/trips", groupId).content(toJson(trip)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The last day cannot be before the first day"));
    }

    @Test
    void updatesAndPausesATrip() throws Exception {
        long tripId = read(mvc.perform(postAs(ana, "/api/groups/{id}/trips", groupId).content(toJson(schoolRun())))
                .andReturn()).get("id").asLong();
        Map<String, Object> changed = schoolRun();
        changed.put("departureTime", "08:00");
        changed.put("days", List.of("MONDAY", "WEDNESDAY"));

        mvc.perform(putAs(ana, "/api/trips/{id}", tripId).content(toJson(changed)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departureTime").value("08:00:00"))
                .andExpect(jsonPath("$.days", hasSize(2)));
        mvc.perform(postAs(ana, "/api/trips/{id}/pause", tripId))
                .andExpect(jsonPath("$.active").value(false));
        mvc.perform(postAs(ana, "/api/trips/{id}/resume", tripId))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void deletesATrip() throws Exception {
        long tripId = read(mvc.perform(postAs(ana, "/api/groups/{id}/trips", groupId).content(toJson(schoolRun())))
                .andReturn()).get("id").asLong();

        mvc.perform(deleteAs(ben, "/api/trips/{id}", tripId)).andExpect(status().isForbidden());
        mvc.perform(deleteAs(ana, "/api/trips/{id}", tripId)).andExpect(status().isNoContent());
        mvc.perform(getAs(ana, "/api/trips/{id}", tripId)).andExpect(status().isNotFound());
    }

    @Test
    void outsidersCannotListTrips() throws Exception {
        String eve = register("eve@example.com", "Eve");

        mvc.perform(getAs(eve, "/api/groups/{id}/trips", groupId)).andExpect(status().isNotFound());
    }

    @Test
    void cancelsTheRidesOfAHoliday() throws Exception {
        long tripId = read(mvc.perform(postAs(ana, "/api/groups/{id}/trips", groupId).content(toJson(schoolRun())))
                .andReturn()).get("id").asLong();

        mvc.perform(postAs(ana, "/api/trips/{id}/cancel-days", tripId)
                        .content(toJson(Map.of("from", "2026-03-09", "to", "2026-03-13", "reason", "Spring break"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelled").value(5));

        mvc.perform(getAs(ben, "/api/groups/{id}/rides", groupId).param("from", "2026-03-09").param("to", "2026-03-16"))
                .andExpect(jsonPath("$[0].status").value("CANCELLED"))
                .andExpect(jsonPath("$[0].cancelReason").value("Spring break"))
                .andExpect(jsonPath("$[4].status").value("CANCELLED"));
    }

    @Test
    void onlyAdminsCancelDays() throws Exception {
        long tripId = read(mvc.perform(postAs(ana, "/api/groups/{id}/trips", groupId).content(toJson(schoolRun())))
                .andReturn()).get("id").asLong();

        mvc.perform(postAs(ben, "/api/trips/{id}/cancel-days", tripId)
                        .content(toJson(Map.of("from", "2026-03-09", "to", "2026-03-13"))))
                .andExpect(status().isForbidden());
        mvc.perform(postAs(ana, "/api/trips/{id}/cancel-days", tripId)
                        .content(toJson(Map.of("from", "2026-03-13", "to", "2026-03-09"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addsTheReturnJourney() throws Exception {
        long tripId = read(mvc.perform(postAs(ana, "/api/groups/{id}/trips", groupId).content(toJson(schoolRun())))
                .andReturn()).get("id").asLong();

        mvc.perform(postAs(ana, "/api/trips/{id}/return", tripId).content(toJson(Map.of("departureTime", "15:30"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Morning school run (return)"))
                .andExpect(jsonPath("$.origin").value("Riverside Primary"))
                .andExpect(jsonPath("$.destination").value("12 Elm St"))
                .andExpect(jsonPath("$.departureTime").value("15:30:00"))
                .andExpect(jsonPath("$.days", hasSize(5)));
        mvc.perform(postAs(ben, "/api/trips/{id}/return", tripId).content(toJson(Map.of("departureTime", "15:30"))))
                .andExpect(status().isForbidden());
    }
}
