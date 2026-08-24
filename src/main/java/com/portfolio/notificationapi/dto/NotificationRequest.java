package com.portfolio.notificationapi.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record NotificationRequest(

        @NotBlank(message = "title is required")
        @Size(max = 120)
        String title,

        @NotBlank(message = "message is required")
        @Size(max = 1000)
        String message,

        @NotBlank(message = "recipient is required")
        @Size(max = 120)
        String recipient,

        @NotNull(message = "channelId is required")
        Long channelId,

        @Future(message = "scheduledAt must be in the future")
        LocalDateTime scheduledAt
) {
}
