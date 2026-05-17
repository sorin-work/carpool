package com.carpool.notification;

import com.carpool.support.IntegrationTest;
import com.carpool.user.User;
import com.carpool.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationCleanupTest extends IntegrationTest {

    @Autowired
    private NotificationService notifications;

    @Autowired
    private NotificationRepository repository;

    @Autowired
    private NotificationCleanup cleanup;

    @Autowired
    private UserRepository users;

    @Test
    void removesOnlyOldNotifications() throws Exception {
        register("ana@example.com", "Ana");
        User ana = users.findByEmailIgnoreCase("ana@example.com").orElseThrow();
        notifications.notify(ana, NotificationType.SEAT_CONFIRMED, "Old", null);
        clock.advance(Duration.ofDays(89));
        notifications.notify(ana, NotificationType.SEAT_CONFIRMED, "Recent", null);
        clock.advance(Duration.ofDays(2));

        assertThat(cleanup.purge()).isEqualTo(1);
        assertThat(repository.findAll()).extracting(Notification::getMessage).containsExactly("Recent");
    }
}
