package com.carpool.user;

import com.carpool.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest extends IntegrationTest {

    @Test
    void registersAndReturnsAToken() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("email", "Ana@Example.com", "displayName", "Ana",
                                "password", "long-enough-password"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("ana@example.com"))
                .andExpect(jsonPath("$.user.displayName").value("Ana"));
    }

    @Test
    void rejectsADuplicateEmailIgnoringCase() throws Exception {
        register("ana@example.com", "Ana");

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("email", "ANA@example.com", "displayName", "Ana 2",
                                "password", "long-enough-password"))))
                .andExpect(status().isConflict());
    }

    @Test
    void validatesTheRegistration() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("email", "not-an-email", "displayName", "", "password", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.email").exists())
                .andExpect(jsonPath("$.fields.displayName").exists())
                .andExpect(jsonPath("$.fields.password").exists());
    }

    @Test
    void logsInWithTheRightPassword() throws Exception {
        register("ben@example.com", "Ben");

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("email", "BEN@example.com", "password", "correct-horse-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.displayName").value("Ben"));
    }

    @Test
    void rejectsAWrongPassword() throws Exception {
        register("ben@example.com", "Ben");

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("email", "ben@example.com", "password", "nope-nope-nope"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void protectedEndpointsNeedAToken() throws Exception {
        mvc.perform(get("/api/groups"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }
}
