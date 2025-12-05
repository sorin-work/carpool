package com.carpool.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Base class for API tests: the full application against H2, one transaction per test that is
 * rolled back afterwards, and a clock pinned to Monday 2 March 2026 06:00 UTC.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(IntegrationTest.TestClockConfig.class)
public abstract class IntegrationTest {

    public static final Instant NOW = Instant.parse("2026-03-02T06:00:00Z");

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    @Autowired
    protected MutableClock clock;

    @BeforeEach
    void resetClock() {
        clock.set(NOW);
    }

    @TestConfiguration
    static class TestClockConfig {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(NOW);
        }
    }

    protected String register(String email, String displayName) throws Exception {
        String body = json.writeValueAsString(new Credentials(email, displayName, "correct-horse-1"));
        MvcResult result = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(body)).andReturn();
        if (result.getResponse().getStatus() != 201) {
            throw new IllegalStateException("register failed: " + result.getResponse().getContentAsString());
        }
        return json.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    protected JsonNode createGroup(String token, String name) throws Exception {
        MvcResult result = mvc.perform(postAs(token, "/api/groups").content(toJson(java.util.Map.of("name", name))))
                .andReturn();
        return read(result);
    }

    protected JsonNode joinGroup(String token, String inviteCode) throws Exception {
        MvcResult result = mvc.perform(postAs(token, "/api/groups/join")
                .content(toJson(java.util.Map.of("inviteCode", inviteCode)))).andReturn();
        return read(result);
    }

    protected JsonNode read(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }

    protected String toJson(Object value) throws Exception {
        return json.writeValueAsString(value);
    }

    protected MockHttpServletRequestBuilder getAs(String token, String url, Object... vars) {
        return get(url, vars).header("Authorization", "Bearer " + token);
    }

    protected MockHttpServletRequestBuilder postAs(String token, String url, Object... vars) {
        return post(url, vars).header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }

    protected MockHttpServletRequestBuilder putAs(String token, String url, Object... vars) {
        return put(url, vars).header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }

    protected MockHttpServletRequestBuilder patchAs(String token, String url, Object... vars) {
        return patch(url, vars).header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }

    protected MockHttpServletRequestBuilder deleteAs(String token, String url, Object... vars) {
        return delete(url, vars).header("Authorization", "Bearer " + token);
    }

    private record Credentials(String email, String displayName, String password) {
    }
}
