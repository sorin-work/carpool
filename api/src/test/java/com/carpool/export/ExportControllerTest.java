package com.carpool.export;

import com.carpool.support.Fixtures;
import com.carpool.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExportControllerTest extends IntegrationTest {

    @Test
    void exportsCompletedRides() throws Exception {
        String ana = register("ana@example.com", "Ana");
        String ben = register("ben@example.com", "Ben");
        JsonNode group = createGroup(ana, "School run");
        long groupId = group.get("id").asLong();
        joinGroup(ben, group.get("inviteCode").asText());
        mvc.perform(postAs(ana, "/api/groups/{id}/trips", groupId).content(toJson(Fixtures.schoolRun())));
        mvc.perform(putAs(ana, "/api/groups/{id}/members/me", groupId).content(toJson(Fixtures.car(3))));
        long rideId = read(mvc.perform(getAs(ana, "/api/groups/{id}/rides", groupId)
                .param("from", "2026-03-03").param("to", "2026-03-04")).andReturn()).get(0).get("id").asLong();
        mvc.perform(postAs(ana, "/api/rides/{id}/driver", rideId));
        mvc.perform(postAs(ben, "/api/rides/{id}/seat", rideId));
        clock.set(Instant.parse("2026-03-05T12:00:00Z"));

        String csv = mvc.perform(getAs(ben, "/api/groups/{id}/rides.csv", groupId)
                        .param("from", "2026-03-01").param("to", "2026-03-08"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(csv.split("\r\n")).containsExactly(
                "date,time,trip,distance_km,driver,riders",
                "2026-03-03,07:45,Morning school run,6.4,Ana,Ben");
    }

    @Test
    void rejectsRangesLongerThanAYear() throws Exception {
        String ana = register("ana@example.com", "Ana");
        long groupId = createGroup(ana, "School run").get("id").asLong();

        mvc.perform(getAs(ana, "/api/groups/{id}/rides.csv", groupId).param("from", "2024-01-01").param("to", "2026-01-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void quotesCellsThatNeedIt() {
        String csv = new CsvWriter().row(List.of("Elm St, north", "say \"hi\"", "plain")).toString();

        assertThat(csv).isEqualTo("\"Elm St, north\",\"say \"\"hi\"\"\",plain\r\n");
    }
}
