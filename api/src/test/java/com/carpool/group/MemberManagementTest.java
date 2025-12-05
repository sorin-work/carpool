package com.carpool.group;

import com.carpool.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MemberManagementTest extends IntegrationTest {

    private String ana;
    private String ben;
    private long groupId;
    private long benMemberId;

    @BeforeEach
    void setUp() throws Exception {
        ana = register("ana@example.com", "Ana");
        ben = register("ben@example.com", "Ben");
        JsonNode group = createGroup(ana, "Office");
        groupId = group.get("id").asLong();
        benMemberId = joinGroup(ben, group.get("inviteCode").asText()).get("myMemberId").asLong();
    }

    @Test
    void membersCannotEditTheGroup() throws Exception {
        mvc.perform(patchAs(ben, "/api/groups/{id}", groupId).content(toJson(Map.of("name", "Mine now"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminsCanEditTheGroupPartially() throws Exception {
        mvc.perform(patchAs(ana, "/api/groups/{id}", groupId).content(toJson(Map.of("description", "Weekdays only"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Office"))
                .andExpect(jsonPath("$.description").value("Weekdays only"));
    }

    @Test
    void promotedMembersCanManage() throws Exception {
        mvc.perform(putAs(ana, "/api/groups/{g}/members/{m}/role", groupId, benMemberId)
                        .content(toJson(Map.of("role", "ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        mvc.perform(patchAs(ben, "/api/groups/{id}", groupId).content(toJson(Map.of("name", "Office pool"))))
                .andExpect(status().isOk());
    }

    @Test
    void ownershipCannotBeAssignedThroughTheRoleEndpoint() throws Exception {
        mvc.perform(putAs(ana, "/api/groups/{g}/members/{m}/role", groupId, benMemberId)
                        .content(toJson(Map.of("role", "OWNER"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rotatingTheInviteCodeInvalidatesTheOldOne() throws Exception {
        String old = read(mvc.perform(getAs(ana, "/api/groups/{id}", groupId)).andReturn()).get("inviteCode").asText();

        mvc.perform(postAs(ana, "/api/groups/{id}/invite-code", groupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inviteCode", not(old)));

        String cara = register("cara@example.com", "Cara");
        mvc.perform(postAs(cara, "/api/groups/join").content(toJson(Map.of("inviteCode", old))))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminsRemoveMembers() throws Exception {
        mvc.perform(deleteAs(ana, "/api/groups/{g}/members/{m}", groupId, benMemberId))
                .andExpect(status().isNoContent());

        mvc.perform(getAs(ben, "/api/groups/{id}", groupId)).andExpect(status().isNotFound());
    }

    @Test
    void membersCannotRemoveOthers() throws Exception {
        long anaMemberId = read(mvc.perform(getAs(ana, "/api/groups/{id}", groupId)).andReturn())
                .get("myMemberId").asLong();

        mvc.perform(deleteAs(ben, "/api/groups/{g}/members/{m}", groupId, anaMemberId))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerMustHandOverBeforeLeaving() throws Exception {
        mvc.perform(postAs(ana, "/api/groups/{id}/leave", groupId)).andExpect(status().isBadRequest());

        mvc.perform(postAs(ana, "/api/groups/{g}/members/{m}/ownership", groupId, benMemberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myRole").value("ADMIN"));
        mvc.perform(postAs(ana, "/api/groups/{id}/leave", groupId)).andExpect(status().isNoContent());

        mvc.perform(getAs(ben, "/api/groups/{id}", groupId))
                .andExpect(jsonPath("$.myRole").value("OWNER"))
                .andExpect(jsonPath("$.members", hasSize(1)));
    }

    @Test
    void membersSetTheirCarAndPickupNote() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("canDrive", true);
        body.put("carSeats", 3);
        body.put("pickupNote", "Corner of Elm St and 3rd");

        mvc.perform(putAs(ben, "/api/groups/{id}/members/me", groupId).content(toJson(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canDrive").value(true))
                .andExpect(jsonPath("$.carSeats").value(3))
                .andExpect(jsonPath("$.pickupNote").value("Corner of Elm St and 3rd"));
    }

    @Test
    void driversNeedASeat() throws Exception {
        mvc.perform(putAs(ben, "/api/groups/{id}/members/me", groupId)
                        .content(toJson(Map.of("canDrive", true, "carSeats", 0))))
                .andExpect(status().isBadRequest());
    }
}
