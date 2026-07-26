package com.carpool.cost;

import com.carpool.support.Fixtures;
import com.carpool.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CostControllerTest extends IntegrationTest {

    private String ana;
    private String ben;
    private String cara;
    private long groupId;

    @BeforeEach
    void setUp() throws Exception {
        ana = register("ana@example.com", "Ana");
        ben = register("ben@example.com", "Ben");
        cara = register("cara@example.com", "Cara");
        JsonNode group = createGroup(ana, "Office");
        groupId = group.get("id").asLong();
        joinGroup(ben, group.get("inviteCode").asText());
        joinGroup(cara, group.get("inviteCode").asText());
        Map<String, Object> trip = Fixtures.schoolRun();
        trip.put("distanceKm", 10);
        mvc.perform(postAs(ana, "/api/groups/{id}/trips", groupId).content(toJson(trip)));
        mvc.perform(patchAs(ana, "/api/groups/{id}", groupId).content(toJson(Map.of("costPerKm", 0.24))));
        mvc.perform(putAs(ana, "/api/groups/{id}/members/me", groupId).content(toJson(Fixtures.car(3))));
        JsonNode rides = read(mvc.perform(getAs(ana, "/api/groups/{id}/rides", groupId)
                .param("from", "2026-03-03").param("to", "2026-03-05")).andReturn());
        // Two rides with Ana driving: Ben and Cara on the first, only Ben on the second.
        long tue = rides.get(0).get("id").asLong();
        long wed = rides.get(1).get("id").asLong();
        mvc.perform(postAs(ana, "/api/rides/{id}/driver", tue));
        mvc.perform(postAs(ana, "/api/rides/{id}/driver", wed));
        mvc.perform(postAs(ben, "/api/rides/{id}/seat", tue));
        mvc.perform(postAs(cara, "/api/rides/{id}/seat", tue));
        mvc.perform(postAs(ben, "/api/rides/{id}/seat", wed));
        clock.set(Instant.parse("2026-03-06T12:00:00Z"));
    }

    @Test
    void balancesForTheMonth() throws Exception {
        // Tue: 2.40 / 3 = 0.80 each from Ben and Cara. Wed: 2.40 / 2 = 1.20 from Ben.
        mvc.perform(getAs(ben, "/api/groups/{id}/costs", groupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-03-01"))
                .andExpect(jsonPath("$.to").value("2026-04-01"))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.totalCents").value(480))
                .andExpect(jsonPath("$.balances[0].displayName").value("Ana"))
                .andExpect(jsonPath("$.balances[0].earnedCents").value(280))
                .andExpect(jsonPath("$.balances[0].balanceCents").value(280))
                .andExpect(jsonPath("$.balances[1].displayName").value("Cara"))
                .andExpect(jsonPath("$.balances[1].balanceCents").value(-80))
                .andExpect(jsonPath("$.balances[2].displayName").value("Ben"))
                .andExpect(jsonPath("$.balances[2].balanceCents").value(-200));
    }

    @Test
    void suggestsSettlements() throws Exception {
        mvc.perform(getAs(cara, "/api/groups/{id}/costs", groupId))
                .andExpect(jsonPath("$.settlements", hasSize(2)))
                .andExpect(jsonPath("$.settlements[0].fromName").value("Ben"))
                .andExpect(jsonPath("$.settlements[0].toName").value("Ana"))
                .andExpect(jsonPath("$.settlements[0].amountCents").value(200))
                .andExpect(jsonPath("$.settlements[1].fromName").value("Cara"))
                .andExpect(jsonPath("$.settlements[1].amountCents").value(80));
    }

    @Test
    void anEmptyPeriodCostsNothing() throws Exception {
        mvc.perform(getAs(ana, "/api/groups/{id}/costs", groupId).param("from", "2026-02-01").param("to", "2026-03-01"))
                .andExpect(jsonPath("$.totalCents").value(0))
                .andExpect(jsonPath("$.settlements", hasSize(0)));
    }

    @Test
    void validatesTheRate() throws Exception {
        mvc.perform(patchAs(ana, "/api/groups/{id}", groupId).content(toJson(Map.of("costPerKm", -1))))
                .andExpect(status().isBadRequest());
        mvc.perform(patchAs(ana, "/api/groups/{id}", groupId).content(toJson(Map.of("currency", "euro"))))
                .andExpect(status().isBadRequest());
    }

    private long memberId(String name) throws Exception {
        for (JsonNode m : read(mvc.perform(getAs(ana, "/api/groups/{id}", groupId)).andReturn()).get("members")) {
            if (m.get("displayName").asText().equals(name)) {
                return m.get("id").asLong();
            }
        }
        throw new IllegalArgumentException(name);
    }

    @Test
    void paymentsSettleTheBalance() throws Exception {
        mvc.perform(postAs(ben, "/api/groups/{id}/payments", groupId).content(toJson(Map.of(
                        "fromMemberId", memberId("Ben"), "toMemberId", memberId("Ana"), "amountCents", 200))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paidOn").value("2026-03-06"));

        mvc.perform(getAs(ana, "/api/groups/{id}/costs", groupId))
                .andExpect(jsonPath("$.payments", hasSize(1)))
                .andExpect(jsonPath("$.balances[0].displayName").value("Ana"))
                .andExpect(jsonPath("$.balances[0].balanceCents").value(80))
                .andExpect(jsonPath("$.settlements", hasSize(1)))
                .andExpect(jsonPath("$.settlements[0].fromName").value("Cara"));
    }

    @Test
    void outsidersOfAPaymentCannotRecordIt() throws Exception {
        mvc.perform(postAs(cara, "/api/groups/{id}/payments", groupId).content(toJson(Map.of(
                        "fromMemberId", memberId("Ben"), "toMemberId", memberId("Ana"), "amountCents", 200))))
                .andExpect(status().isForbidden());
        mvc.perform(postAs(ana, "/api/groups/{id}/payments", groupId).content(toJson(Map.of(
                        "fromMemberId", memberId("Ana"), "toMemberId", memberId("Ana"), "amountCents", 200))))
                .andExpect(status().isBadRequest());
        mvc.perform(postAs(ana, "/api/groups/{id}/payments", groupId).content(toJson(Map.of(
                        "fromMemberId", memberId("Ben"), "toMemberId", memberId("Ana"), "amountCents", 0))))
                .andExpect(status().isBadRequest());
    }
}
