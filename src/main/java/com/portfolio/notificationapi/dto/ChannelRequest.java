package com.portfolio.notificationapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChannelRequest(

        @NotBlank(message = "name is required")
        @Size(max = 30)
        String name,

        @Size(max = 255)
        String description,

        boolean active
) {
}
