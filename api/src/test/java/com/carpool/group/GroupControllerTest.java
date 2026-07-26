package com.carpool.group;

import com.carpool.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GroupControllerTest extends IntegrationTest {

    @Test
    void createsAGroupWithTheCreatorAsOwner() throws Exception {
        String ana = register("ana@example.com", "Ana");

        mvc.perform(postAs(ana, "/api/groups").content(toJson(Map.of("name", "Elm St school run",
                        "description", "Mornings to Riverside Primary", "timezone", "Europe/Berlin"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Elm St school run"))
                .andExpect(jsonPath("$.timezone").value("Europe/Berlin"))
                .andExpect(jsonPath("$.inviteCode").isNotEmpty())
                .andExpect(jsonPath("$.myRole").value("OWNER"))
                .andExpect(jsonPath("$.members", hasSize(1)))
                .andExpect(jsonPath("$.members[0].displayName").value("Ana"));
    }

    @Test
    void defaultsTheTimeZone() throws Exception {
        String ana = register("ana@example.com", "Ana");

        mvc.perform(postAs(ana, "/api/groups").content(toJson(Map.of("name", "Office"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.timezone").value("Europe/Bucharest"));
    }

    @Test
    void rejectsAnUnknownTimeZone() throws Exception {
        String ana = register("ana@example.com", "Ana");

        mvc.perform(postAs(ana, "/api/groups").content(toJson(Map.of("name", "Office", "timezone", "Mars/Olympus"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unknown time zone: Mars/Olympus"));
    }

    @Test
    void joinsWithAnInviteCodeTypedInLowerCase() throws Exception {
        String ana = register("ana@example.com", "Ana");
        String ben = register("ben@example.com", "Ben");
        JsonNode group = createGroup(ana, "Office");
        String code = group.get("inviteCode").asText().toLowerCase();

        mvc.perform(postAs(ben, "/api/groups/join").content(toJson(Map.of("inviteCode", code))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myRole").value("MEMBER"))
                .andExpect(jsonPath("$.members", hasSize(2)));

        mvc.perform(getAs(ben, "/api/groups"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].memberCount").value(2));
    }

    @Test
    void joiningTwiceIsHarmless() throws Exception {
        String ana = register("ana@example.com", "Ana");
        JsonNode group = createGroup(ana, "Office");

        mvc.perform(postAs(ana, "/api/groups/join").content(toJson(Map.of("inviteCode", group.get("inviteCode").asText()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myRole").value("OWNER"))
                .andExpect(jsonPath("$.members", hasSize(1)));
    }

    @Test
    void unknownInviteCodeIsNotFound() throws Exception {
        String ben = register("ben@example.com", "Ben");

        mvc.perform(postAs(ben, "/api/groups/join").content(toJson(Map.of("inviteCode", "ZZZZZZZZ"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void outsidersCannotSeeAGroup() throws Exception {
        String ana = register("ana@example.com", "Ana");
        String eve = register("eve@example.com", "Eve");
        JsonNode group = createGroup(ana, "Office");

        mvc.perform(getAs(eve, "/api/groups/{id}", group.get("id").asLong()))
                .andExpect(status().isNotFound());
    }

    @Test
    void previewsAGroupFromItsInviteCode() throws Exception {
        String ana = register("ana@example.com", "Ana");
        String ben = register("ben@example.com", "Ben");
        JsonNode group = createGroup(ana, "Office");

        mvc.perform(getAs(ben, "/api/groups/invite/{code}", group.get("inviteCode").asText().toLowerCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Office"))
                .andExpect(jsonPath("$.memberCount").value(1))
                .andExpect(jsonPath("$.alreadyMember").value(false));
        mvc.perform(getAs(ben, "/api/groups/invite/{code}", "NOPE2345")).andExpect(status().isNotFound());
    }
}
