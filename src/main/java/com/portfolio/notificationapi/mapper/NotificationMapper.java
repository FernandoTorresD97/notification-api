package com.portfolio.notificationapi.mapper;

import com.portfolio.notificationapi.dto.ChannelResponse;
import com.portfolio.notificationapi.dto.NotificationResponse;
import com.portfolio.notificationapi.entity.Channel;
import com.portfolio.notificationapi.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getRecipient(),
                notification.getChannel().getName(),
                notification.getStatus(),
                notification.getScheduledAt(),
                notification.getSentAt(),
                notification.getCreatedAt(),
                notification.getUpdatedAt()
        );
    }

    public ChannelResponse toResponse(Channel channel) {
        return new ChannelResponse(
                channel.getId(),
                channel.getName(),
                channel.getDescription(),
                channel.isActive()
        );
    }
}
