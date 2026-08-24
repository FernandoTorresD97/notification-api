package com.portfolio.notificationapi.dto;

import com.portfolio.notificationapi.entity.NotificationStatus;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String title,
        String message,
        String recipient,
        String channelName,
        NotificationStatus status,
        LocalDateTime scheduledAt,
        LocalDateTime sentAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
