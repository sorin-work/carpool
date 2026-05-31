package com.carpool.notification;

import com.carpool.support.IntegrationTest;
import com.carpool.user.User;
import com.carpool.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationControllerTest extends IntegrationTest {

    @Autowired
    private NotificationService notifications;

    @Autowired
    private UserRepository users;

    private String token;
    private User user;

    @BeforeEach
    void setUp() throws Exception {
        token = register("ana@example.com", "Ana");
        user = users.findByEmailIgnoreCase("ana@example.com").orElseThrow();
    }

    private void send(int count) {
        for (int i = 1; i <= count; i++) {
            notifications.notify(user, NotificationType.SEAT_CONFIRMED, "Message " + i, null);
            clock.advance(Duration.ofMinutes(1));
        }
    }

    @Test
    void newestFirst() throws Exception {
        send(3);

        mvc.perform(getAs(token, "/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(jsonPath("$.items[0].message").value("Message 3"))
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.items[0].read").value(false));
    }

    @Test
    void pagesThroughTheList() throws Exception {
        send(5);

        mvc.perform(getAs(token, "/api/notifications").param("size", "2"))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].message").value("Message 5"));
        mvc.perform(getAs(token, "/api/notifications").param("page", "2").param("size", "2"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].message").value("Message 1"));
    }

    @Test
    void offersTheLastPartlyFilledPage() throws Exception {
        send(5);

        mvc.perform(getAs(token, "/api/notifications").param("size", "2"))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.hasNext").value(true));
        mvc.perform(getAs(token, "/api/notifications").param("page", "1").param("size", "2"))
                .andExpect(jsonPath("$.hasNext").value(true));
        mvc.perform(getAs(token, "/api/notifications").param("page", "2").param("size", "2"))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void rejectsSillyPageSizes() throws Exception {
        mvc.perform(getAs(token, "/api/notifications").param("size", "0")).andExpect(status().isBadRequest());
        mvc.perform(getAs(token, "/api/notifications").param("size", "500")).andExpect(status().isBadRequest());
    }

    @Test
    void countsAndMarksUnread() throws Exception {
        send(2);
        long first = read(mvc.perform(getAs(token, "/api/notifications")).andReturn()).get("items").get(0).get("id").asLong();

        mvc.perform(getAs(token, "/api/notifications/unread-count")).andExpect(jsonPath("$.count").value(2));
        mvc.perform(postAs(token, "/api/notifications/{id}/read", first)).andExpect(jsonPath("$.read").value(true));
        mvc.perform(getAs(token, "/api/notifications/unread-count")).andExpect(jsonPath("$.count").value(1));
        mvc.perform(postAs(token, "/api/notifications/read-all")).andExpect(jsonPath("$.updated").value(1));
        mvc.perform(getAs(token, "/api/notifications/unread-count")).andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void cannotReadSomeoneElsesNotification() throws Exception {
        send(1);
        long id = read(mvc.perform(getAs(token, "/api/notifications")).andReturn()).get("items").get(0).get("id").asLong();
        String ben = register("ben@example.com", "Ben");

        mvc.perform(postAs(ben, "/api/notifications/{id}/read", id)).andExpect(status().isNotFound());
    }
}
