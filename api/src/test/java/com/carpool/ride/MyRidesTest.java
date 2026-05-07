package com.carpool.ride;

import com.carpool.support.Fixtures;
import com.carpool.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MyRidesTest extends IntegrationTest {

    @Test
    void collectsMyRidesFromEveryGroup() throws Exception {
        String ana = register("ana@example.com", "Ana");
        String ben = register("ben@example.com", "Ben");
        JsonNode school = createGroup(ana, "School run");
        JsonNode office = createGroup(ben, "Office");
        joinGroup(ben, school.get("inviteCode").asText());
        long schoolId = school.get("id").asLong();
        long officeId = office.get("id").asLong();

        mvc.perform(postAs(ana, "/api/groups/{id}/trips", schoolId).content(toJson(Fixtures.schoolRun())));
        Map<String, Object> commute = Fixtures.schoolRun();
        commute.put("name", "Commute");
        commute.put("departureTime", "08:30");
        mvc.perform(postAs(ben, "/api/groups/{id}/trips", officeId).content(toJson(commute)));
        mvc.perform(putAs(ben, "/api/groups/{id}/members/me", officeId).content(toJson(Fixtures.car(2))));

        long schoolRide = read(mvc.perform(getAs(ben, "/api/groups/{id}/rides", schoolId)
                .param("from", "2026-03-03").param("to", "2026-03-04")).andReturn()).get(0).get("id").asLong();
        long officeRide = read(mvc.perform(getAs(ben, "/api/groups/{id}/rides", officeId)
                .param("from", "2026-03-04").param("to", "2026-03-05")).andReturn()).get(0).get("id").asLong();
        mvc.perform(postAs(ben, "/api/rides/{id}/seat", schoolRide));
        mvc.perform(postAs(ben, "/api/rides/{id}/driver", officeRide));

        mvc.perform(getAs(ben, "/api/me/rides"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].groupName").value("School run"))
                .andExpect(jsonPath("$[0].driving").value(false))
                .andExpect(jsonPath("$[0].ride.myBooking").value("PENDING"))
                .andExpect(jsonPath("$[1].groupName").value("Office"))
                .andExpect(jsonPath("$[1].driving").value(true));
    }

    @Test
    void emptyWhenIHaveNothingPlanned() throws Exception {
        String ana = register("ana@example.com", "Ana");
        createGroup(ana, "School run");

        mvc.perform(getAs(ana, "/api/me/rides")).andExpect(jsonPath("$", hasSize(0)));
    }
}
