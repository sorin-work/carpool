package com.carpool.ride;

import com.carpool.support.Fixtures;
import com.carpool.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class DriverRemindersTest extends IntegrationTest {

    @Autowired
    private DriverReminders reminders;

    @Test
    void remindsTheDriverOnceTheDayBefore() throws Exception {
        String ana = register("ana@example.com", "Ana");
        String ben = register("ben@example.com", "Ben");
        JsonNode group = createGroup(ana, "School run");
        long groupId = group.get("id").asLong();
        joinGroup(ben, group.get("inviteCode").asText());
        mvc.perform(postAs(ana, "/api/groups/{id}/trips", groupId).content(toJson(Fixtures.schoolRun())));
        mvc.perform(putAs(ana, "/api/groups/{id}/members/me", groupId).content(toJson(Fixtures.car(3))));
        JsonNode rides = read(mvc.perform(getAs(ana, "/api/groups/{id}/rides", groupId)
                .param("from", "2026-03-03").param("to", "2026-03-05")).andReturn());
        mvc.perform(postAs(ana, "/api/rides/{id}/driver", rides.get(0).get("id").asLong()));
        mvc.perform(postAs(ana, "/api/rides/{id}/driver", rides.get(1).get("id").asLong()));
        mvc.perform(postAs(ben, "/api/rides/{id}/seat", rides.get(0).get("id").asLong()));

        // Monday 18:00 UTC: Tuesday's ride is inside the next 24 hours, Wednesday's is not.
        clock.set(Instant.parse("2026-03-02T18:00:00Z"));
        assertThat(reminders.send()).isEqualTo(1);
        assertThat(reminders.send()).isZero();

        mvc.perform(getAs(ana, "/api/notifications"))
                .andExpect(jsonPath("$.items[0].type").value("DRIVER_REMINDER"))
                .andExpect(jsonPath("$.items[0].message", containsString("Tue 3 Mar at 07:45 (1 rider)")));
    }
}
