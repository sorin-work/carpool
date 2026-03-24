package com.carpool.seed;

import com.carpool.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Own database: the seeder commits outside the test transaction.
@TestPropertySource(properties = {
        "app.seed-demo=true",
        "spring.datasource.url=jdbc:h2:mem:seed;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
})
class DemoDataSeederTest extends IntegrationTest {

    @Autowired
    private DemoDataSeeder seeder;

    private String login() throws Exception {
        return read(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("email", DemoDataSeeder.DEMO_EMAIL, "password", DemoDataSeeder.DEMO_PASSWORD))))
                .andExpect(status().isOk()).andReturn()).get("token").asText();
    }

    @Test
    void theDemoAccountHasGroupsAndHistory() throws Exception {
        String token = login();

        JsonNode groups = read(mvc.perform(getAs(token, "/api/groups")).andReturn());
        assertThat(groups.size()).isEqualTo(2);

        long schoolId = groups.findValues("id").get(0).asLong();
        JsonNode stats = read(mvc.perform(getAs(token, "/api/groups/{id}/stats", schoolId)).andReturn());
        assertThat(stats.get("rides").asInt()).isGreaterThan(20);
    }

    @Test
    void runningAgainDoesNothing() throws Exception {
        seeder.run(null);

        JsonNode groups = read(mvc.perform(getAs(login(), "/api/groups")).andReturn());
        assertThat(groups.size()).isEqualTo(2);
    }
}
