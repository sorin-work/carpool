package com.carpool.common;

import com.carpool.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ErrorResponsesTest extends IntegrationTest {

    @Test
    void unknownEndpointsAreJson404s() throws Exception {
        String token = register("ana@example.com", "Ana");

        mvc.perform(getAs(token, "/api/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No such endpoint"))
                .andExpect(jsonPath("$.path").value("/api/does-not-exist"));
    }

    @Test
    void wrongMethodsAre405s() throws Exception {
        String token = register("ana@example.com", "Ana");

        mvc.perform(deleteAs(token, "/api/groups"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void badIdsAre400s() throws Exception {
        String token = register("ana@example.com", "Ana");

        mvc.perform(getAs(token, "/api/groups/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'groupId'"));
    }

    @Test
    void malformedBodiesAre400s() throws Exception {
        String token = register("ana@example.com", "Ana");

        mvc.perform(postAs(token, "/api/groups").content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }
}
