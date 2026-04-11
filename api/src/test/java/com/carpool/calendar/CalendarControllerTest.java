package com.carpool.calendar;

import com.carpool.support.Fixtures;
import com.carpool.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;


import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CalendarControllerTest extends IntegrationTest {

    @Test
    void listsTheRidesIDriveOrRide() throws Exception {
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
        mvc.perform(postAs(ben, "/api/rides/{id}/seat", rides.get(0).get("id").asLong()));

        String ics = mvc.perform(getAs(ana, "/api/groups/{id}/calendar.ics", groupId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/calendar;charset=utf-8"))
                .andReturn().getResponse().getContentAsString();

        assertThat(ics).startsWith("BEGIN:VCALENDAR\r\n").endsWith("END:VCALENDAR\r\n");
        assertThat(ics).contains("SUMMARY:Drive: Morning school run", "DTSTART:20260303T054500Z",
                "DESCRIPTION:1 passenger(s): Ben");
        assertThat(ics.split("BEGIN:VEVENT")).hasSize(2);
    }
}
