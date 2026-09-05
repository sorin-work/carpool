package com.carpool.swap;

import com.carpool.support.Fixtures;
import com.carpool.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SwapControllerTest extends IntegrationTest {

    private String ana;
    private String ben;
    private long groupId;
    private long benMemberId;
    private long tuesday;
    private long wednesday;

    @BeforeEach
    void setUp() throws Exception {
        ana = register("ana@example.com", "Ana");
        ben = register("ben@example.com", "Ben");
        JsonNode group = createGroup(ana, "School run");
        groupId = group.get("id").asLong();
        benMemberId = joinGroup(ben, group.get("inviteCode").asText()).get("myMemberId").asLong();
        mvc.perform(postAs(ana, "/api/groups/{id}/trips", groupId).content(toJson(Fixtures.schoolRun())));
        mvc.perform(putAs(ana, "/api/groups/{id}/members/me", groupId).content(toJson(Fixtures.car(3))));
        mvc.perform(putAs(ben, "/api/groups/{id}/members/me", groupId).content(toJson(Fixtures.car(2))));
        JsonNode rides = read(mvc.perform(getAs(ana, "/api/groups/{id}/rides", groupId)
                .param("from", "2026-03-03").param("to", "2026-03-05")).andReturn());
        tuesday = rides.get(0).get("id").asLong();
        wednesday = rides.get(1).get("id").asLong();
        mvc.perform(postAs(ana, "/api/rides/{id}/driver", tuesday));
        mvc.perform(postAs(ben, "/api/rides/{id}/driver", wednesday));
    }

    private long askBen(Long offeredRideId) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("targetMemberId", benMemberId);
        body.put("offeredRideId", offeredRideId);
        body.put("message", "Dentist on Tuesday morning");
        return read(mvc.perform(postAs(ana, "/api/rides/{id}/swaps", tuesday).content(toJson(body)))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();
    }

    @Test
    void theTargetIsNotified() throws Exception {
        askBen(wednesday);

        mvc.perform(getAs(ben, "/api/notifications"))
                .andExpect(jsonPath("$.items[0].type").value("SWAP_REQUESTED"))
                .andExpect(jsonPath("$.items[0].message", containsString("**Ana** asks if you can drive")))
                .andExpect(jsonPath("$.items[0].message", containsString("taking **Morning school run** on Wed 4 Mar")));
    }

    @Test
    void acceptingSwapsBothRides() throws Exception {
        long swapId = askBen(wednesday);

        mvc.perform(postAs(ben, "/api/swaps/{id}/accept", swapId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        mvc.perform(getAs(ana, "/api/rides/{id}", tuesday))
                .andExpect(jsonPath("$.driver.displayName").value("Ben"))
                .andExpect(jsonPath("$.seatsOffered").value(2));
        mvc.perform(getAs(ana, "/api/rides/{id}", wednesday))
                .andExpect(jsonPath("$.driver.displayName").value("Ana"));
        mvc.perform(getAs(ana, "/api/notifications"))
                .andExpect(jsonPath("$.items[0].type").value("SWAP_ACCEPTED"));
    }

    @Test
    void aOneWaySwapJustHandsTheRideOver() throws Exception {
        long swapId = askBen(null);

        mvc.perform(postAs(ben, "/api/swaps/{id}/accept", swapId)).andExpect(status().isOk());

        mvc.perform(getAs(ana, "/api/rides/{id}", tuesday)).andExpect(jsonPath("$.driver.displayName").value("Ben"));
        mvc.perform(getAs(ana, "/api/rides/{id}", wednesday)).andExpect(jsonPath("$.driver.displayName").value("Ben"));
    }

    @Test
    void decliningLeavesTheRidesAlone() throws Exception {
        long swapId = askBen(wednesday);

        mvc.perform(postAs(ben, "/api/swaps/{id}/decline", swapId))
                .andExpect(jsonPath("$.status").value("DECLINED"));

        mvc.perform(getAs(ana, "/api/rides/{id}", tuesday)).andExpect(jsonPath("$.driver.displayName").value("Ana"));
        mvc.perform(postAs(ben, "/api/swaps/{id}/accept", swapId)).andExpect(status().isConflict());
    }

    @Test
    void onlyTheTargetAccepts() throws Exception {
        String cara = register("cara@example.com", "Cara");
        joinGroup(cara, read(mvc.perform(getAs(ana, "/api/groups/{id}", groupId)).andReturn()).get("inviteCode").asText());
        long swapId = askBen(wednesday);

        mvc.perform(postAs(cara, "/api/swaps/{id}/accept", swapId)).andExpect(status().isForbidden());
        mvc.perform(postAs(ana, "/api/swaps/{id}/accept", swapId)).andExpect(status().isForbidden());

        mvc.perform(getAs(ana, "/api/rides/{id}", tuesday)).andExpect(jsonPath("$.driver.displayName").value("Ana"));
        mvc.perform(getAs(ben, "/api/groups/{id}/swaps", groupId)).andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void onlyTheTargetDeclines() throws Exception {
        long swapId = askBen(null);

        mvc.perform(postAs(ana, "/api/swaps/{id}/decline", swapId)).andExpect(status().isForbidden());
    }

    @Test
    void theRequesterWithdraws() throws Exception {
        long swapId = askBen(null);

        mvc.perform(postAs(ben, "/api/swaps/{id}/cancel", swapId)).andExpect(status().isForbidden());
        mvc.perform(postAs(ana, "/api/swaps/{id}/cancel", swapId)).andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void onlyTheDriverAsks() throws Exception {
        long anaMemberId = read(mvc.perform(getAs(ana, "/api/groups/{id}", groupId)).andReturn()).get("myMemberId").asLong();

        mvc.perform(postAs(ben, "/api/rides/{id}/swaps", tuesday).content(toJson(Map.of("targetMemberId", anaMemberId))))
                .andExpect(status().isForbidden());
    }

    @Test
    void theOfferedRideMustBeTheTargets() throws Exception {
        Map<String, Object> body = Map.of("targetMemberId", benMemberId, "offeredRideId", tuesday);

        mvc.perform(postAs(ana, "/api/rides/{id}/swaps", tuesday).content(toJson(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void askingTheSamePersonTwiceIsAConflict() throws Exception {
        askBen(null);

        mvc.perform(postAs(ana, "/api/rides/{id}/swaps", tuesday).content(toJson(Map.of("targetMemberId", benMemberId))))
                .andExpect(status().isConflict());
    }

    @Test
    void bothSidesSeeTheSwap() throws Exception {
        askBen(wednesday);

        mvc.perform(getAs(ana, "/api/groups/{id}/swaps", groupId)).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(getAs(ben, "/api/groups/{id}/swaps", groupId))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].requesterName").value("Ana"))
                .andExpect(jsonPath("$[0].offeredRide.date").value("2026-03-04"));
    }

    @Test
    void theNewDriverGivesUpTheirOwnSeat() throws Exception {
        mvc.perform(postAs(ben, "/api/rides/{id}/seat", tuesday));
        long swapId = askBen(null);

        mvc.perform(postAs(ben, "/api/swaps/{id}/accept", swapId)).andExpect(status().isOk());

        mvc.perform(getAs(ben, "/api/rides/{id}", tuesday))
                .andExpect(jsonPath("$.driver.displayName").value("Ben"))
                .andExpect(jsonPath("$.myBooking").doesNotExist())
                .andExpect(jsonPath("$.seatsTaken").value(0));
    }

    @Test
    void cancellingTheRideWithdrawsItsSwaps() throws Exception {
        long swapId = askBen(null);

        mvc.perform(postAs(ana, "/api/rides/{id}/cancel", tuesday)).andExpect(status().isOk());

        mvc.perform(getAs(ben, "/api/groups/{id}/swaps", groupId)).andExpect(jsonPath("$[0].status").value("CANCELLED"));
        mvc.perform(postAs(ben, "/api/swaps/{id}/accept", swapId)).andExpect(status().isConflict());
    }
}
