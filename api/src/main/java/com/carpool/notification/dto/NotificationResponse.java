package com.carpool.notification.dto;

import com.carpool.notification.Notification;
import com.carpool.notification.NotificationType;

import java.time.Instant;

public record NotificationResponse(Long id, NotificationType type, String message, String link, Instant createdAt,
                                   boolean read) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getMessage(), n.getLink(), n.getCreatedAt(), n.isRead());
    }
}
