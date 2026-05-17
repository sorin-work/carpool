package com.carpool.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;

/** Notifications are only useful for a while; drop the old ones every night. */
@Component
public class NotificationCleanup {

    static final Duration KEEP = Duration.ofDays(90);

    private static final Logger log = LoggerFactory.getLogger(NotificationCleanup.class);

    private final NotificationRepository notifications;
    private final Clock clock;

    public NotificationCleanup(NotificationRepository notifications, Clock clock) {
        this.notifications = notifications;
        this.clock = clock;
    }

    @Scheduled(cron = "${app.notification-cleanup-cron:0 30 3 * * *}")
    @Transactional
    public int purge() {
        int removed = notifications.deleteOlderThan(clock.instant().minus(KEEP));
        if (removed > 0) {
            log.info("Removed {} notifications older than {} days", removed, KEEP.toDays());
        }
        return removed;
    }
}
