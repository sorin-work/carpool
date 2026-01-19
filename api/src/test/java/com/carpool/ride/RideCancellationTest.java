package com.carpool.ride;

import com.carpool.support.Fixtures;
import com.carpool.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RideCancellationTest extends IntegrationTest {

    private String ana;
    private String ben;
    private String cara;
    private long groupId;
    private long rideId;

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
        mvc.perform(putAs(ben, "/api/groups/{id}/members/me", groupId).content(toJson(Fixtures.car(3))));
        rideId = read(mvc.perform(getAs(ana, "/api/groups/{id}/rides", groupId)
                .param("from", "2026-03-03").param("to", "2026-03-04")).andReturn()).get(0).get("id").asLong();
        mvc.perform(postAs(ben, "/api/rides/{id}/driver", rideId));
        mvc.perform(postAs(cara, "/api/rides/{id}/seat", rideId));
    }

    @Test
    void theDriverCancelsAndRidersAreTold() throws Exception {
        mvc.perform(postAs(ben, "/api/rides/{id}/cancel", rideId).content(toJson(Map.of("reason", "Car in the garage"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelReason").value("Car in the garage"));

        mvc.perform(getAs(cara, "/api/notifications"))
                .andExpect(jsonPath("$.items[0].type").value("RIDE_CANCELLED"))
                .andExpect(jsonPath("$.items[0].message",
                        containsString("**Morning school run** on Tue 3 Mar at 07:45 was cancelled by **Ben**: Car in the garage")))
                .andExpect(jsonPath("$.items[0].link").value("/groups/" + groupId + "/rides/" + rideId));
    }

    @Test
    void theOneCancellingIsNotNotified() throws Exception {
        mvc.perform(postAs(ben, "/api/rides/{id}/cancel", rideId));

        mvc.perform(getAs(ben, "/api/notifications")).andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void ridersCannotCancel() throws Exception {
        mvc.perform(postAs(cara, "/api/rides/{id}/cancel", rideId)).andExpect(status().isForbidden());
    }

    @Test
    void adminsCanCancelAndReinstate() throws Exception {
        mvc.perform(postAs(ana, "/api/rides/{id}/cancel", rideId)).andExpect(status().isOk());

        mvc.perform(postAs(ana, "/api/rides/{id}/reinstate", rideId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
        mvc.perform(getAs(ben, "/api/notifications"))
                .andExpect(jsonPath("$.items[0].type").value("RIDE_REINSTATED"));
    }

    @Test
    void cancelledRidesTakeNoBookings() throws Exception {
        String dan = register("dan@example.com", "Dan");
        joinGroup(dan, read(mvc.perform(getAs(ana, "/api/groups/{id}", groupId)).andReturn()).get("inviteCode").asText());
        mvc.perform(postAs(ben, "/api/rides/{id}/cancel", rideId));

        mvc.perform(postAs(dan, "/api/rides/{id}/seat", rideId)).andExpect(status().isConflict());
    }

    @Test
    void ridersAreToldWhenTheDriverStepsDown() throws Exception {
        mvc.perform(deleteAs(ben, "/api/rides/{id}/driver", rideId));

        mvc.perform(getAs(cara, "/api/notifications"))
                .andExpect(jsonPath("$.items[0].type").value("DRIVER_LEFT"));
        mvc.perform(getAs(cara, "/api/rides/{id}", rideId))
                .andExpect(jsonPath("$.myBooking").value("PENDING"));
    }

    @Test
    void waitingRidersAreToldWhenTheyGetASeat() throws Exception {
        mvc.perform(deleteAs(ben, "/api/rides/{id}/driver", rideId));
        mvc.perform(postAs(ben, "/api/rides/{id}/driver", rideId));

        mvc.perform(getAs(cara, "/api/notifications"))
                .andExpect(jsonPath("$.items[0].type").value("SEAT_CONFIRMED"))
                .andExpect(jsonPath("$.items[0].message", containsString("with **Ben**")));
    }
}
