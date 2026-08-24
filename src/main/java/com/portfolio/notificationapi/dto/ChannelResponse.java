package com.portfolio.notificationapi.dto;

public record ChannelResponse(
        Long id,
        String name,
        String description,
        boolean active
) {
}
