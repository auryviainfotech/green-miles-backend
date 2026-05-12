package com.greenmiles.backend.notification.dto;

import com.greenmiles.backend.notification.NotificationType;
import java.time.Instant;

public record NotificationResponse(
        Long notificationId, Long userId, String title, String message, NotificationType type, Boolean isRead, Instant createdAt) {}
