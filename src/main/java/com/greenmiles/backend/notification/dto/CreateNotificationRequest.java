package com.greenmiles.backend.notification.dto;

import com.greenmiles.backend.notification.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateNotificationRequest(
        @NotNull Long userId, @NotBlank String title, @NotBlank String message, @NotNull NotificationType type) {}
