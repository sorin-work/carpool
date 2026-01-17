package com.carpool.notification;

import com.carpool.common.ApiException;
import com.carpool.common.PageResponse;
import com.carpool.notification.dto.NotificationResponse;
import com.carpool.user.User;
import org.springframework.data.domain.PageRequest;
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

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(Long userId, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw ApiException.badRequest("page must be >= 0 and size between 1 and 100");
        }
        return PageResponse.from(notifications.findByUserIdOrderByCreatedAtDescIdDesc(userId, PageRequest.of(page, size)),
                NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notifications.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public NotificationResponse markRead(Long id, Long userId) {
        Notification notification = notifications.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("Notification"));
        notification.markRead(clock.instant());
        return NotificationResponse.from(notification);
    }

    @Transactional
    public int markAllRead(Long userId) {
        return notifications.markAllRead(userId, clock.instant());
    }
}
