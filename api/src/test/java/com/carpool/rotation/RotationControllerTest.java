package com.carpool.rotation;

import com.carpool.support.Fixtures;
import com.carpool.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RotationControllerTest extends IntegrationTest {

    private String ana;
    private String ben;
    private String cara;
    private long groupId;

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
    }

    private JsonNode preview(String token, String from, String to) throws Exception {
        return read(mvc.perform(getAs(token, "/api/groups/{id}/rotation", groupId).param("from", from).param("to", to))
                .andExpect(status().isOk()).andReturn());
    }

    private static Map<String, Integer> counts(JsonNode proposals) {
        Map<String, Integer> counts = new HashMap<>();
        proposals.forEach(p -> counts.merge(p.get("displayName").asText(), 1, Integer::sum));
        return counts;
    }

    @Test
    void previewSplitsTheWeekBetweenDrivers() throws Exception {
        JsonNode proposals = preview(cara, "2026-03-03", "2026-03-10");

        assertThat(proposals.size()).isEqualTo(5);
        Map<String, Integer> counts = counts(proposals);
        assertThat(counts.keySet()).containsExactlyInAnyOrder("Ana", "Ben");
        assertThat(Math.abs(counts.get("Ana") - counts.get("Ben"))).isLessThanOrEqualTo(1);
    }

    @Test
    void previewDoesNotAssignAnything() throws Exception {
        preview(ana, "2026-03-03", "2026-03-10");

        mvc.perform(getAs(ana, "/api/groups/{id}/rides", groupId).param("from", "2026-03-03").param("to", "2026-03-10"))
                .andExpect(jsonPath("$[0].driver").doesNotExist());
    }

    @Test
    void applyingAssignsDriversAndConfirmsRiders() throws Exception {
        long rideId = read(mvc.perform(getAs(cara, "/api/groups/{id}/rides", groupId)
                .param("from", "2026-03-03").param("to", "2026-03-04")).andReturn()).get(0).get("id").asLong();
        mvc.perform(postAs(cara, "/api/rides/{id}/seat", rideId));

        mvc.perform(postAs(ana, "/api/groups/{id}/rotation/apply", groupId).param("from", "2026-03-03").param("to", "2026-03-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));

        mvc.perform(getAs(cara, "/api/rides/{id}", rideId))
                .andExpect(jsonPath("$.driver.displayName").exists())
                .andExpect(jsonPath("$.myBooking").value("CONFIRMED"));
        assertThat(preview(ana, "2026-03-03", "2026-03-10").size()).isZero();
    }

    @Test
    void onlyAdminsApply() throws Exception {
        mvc.perform(postAs(ben, "/api/groups/{id}/rotation/apply", groupId)).andExpect(status().isForbidden());
    }

    @Test
    void ridesAlreadyTakenCountTowardsFairness() throws Exception {
        JsonNode week = read(mvc.perform(getAs(ana, "/api/groups/{id}/rides", groupId)
                .param("from", "2026-03-03").param("to", "2026-03-06")).andReturn());
        for (JsonNode ride : week) {
            mvc.perform(postAs(ana, "/api/rides/{id}/driver", ride.get("id").asLong()));
        }

        JsonNode proposals = preview(ana, "2026-03-06", "2026-03-10");

        assertThat(counts(proposals)).containsOnly(Map.entry("Ben", 2));
    }

    @Test
    void absentMembersAreSkipped() throws Exception {
        mvc.perform(postAs(ben, "/api/groups/{id}/absences", groupId)
                        .content(toJson(Map.of("startsOn", "2026-03-03", "endsOn", "2026-03-09", "note", "Ski trip"))))
                .andExpect(status().isCreated());

        assertThat(counts(preview(ana, "2026-03-03", "2026-03-10"))).containsOnly(Map.entry("Ana", 5));
    }

    @Test
    void absencesAreListedAndRemovable() throws Exception {
        long id = read(mvc.perform(postAs(ben, "/api/groups/{id}/absences", groupId)
                .content(toJson(Map.of("startsOn", "2026-03-10", "endsOn", "2026-03-12")))).andReturn()).get("id").asLong();

        mvc.perform(getAs(cara, "/api/groups/{id}/absences", groupId))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].displayName").value("Ben"));
        mvc.perform(deleteAs(cara, "/api/absences/{id}", id)).andExpect(status().isForbidden());
        mvc.perform(deleteAs(ben, "/api/absences/{id}", id)).andExpect(status().isNoContent());
    }

    @Test
    void rejectsAnAbsenceThatEndsBeforeItStarts() throws Exception {
        mvc.perform(postAs(ben, "/api/groups/{id}/absences", groupId)
                        .content(toJson(Map.of("startsOn", "2026-03-10", "endsOn", "2026-03-09"))))
                .andExpect(status().isBadRequest());
    }
}
