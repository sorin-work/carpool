package com.carpool.notification;

import com.carpool.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Collection;

@Service
public class NotificationService {

    private final NotificationRepository notifications;
    private final Clock clock;

    public NotificationService(NotificationRepository notifications, Clock clock) {
        this.notifications = notifications;
        this.clock = clock;
    }

    @Transactional
    public void notify(User user, NotificationType type, String message, String link) {
        notifications.save(new Notification(user, type, message, link, clock.instant()));
    }

    @Transactional
    public void notifyAll(Collection<User> users, NotificationType type, String message, String link) {
        users.forEach(user -> notify(user, type, message, link));
    }
}
