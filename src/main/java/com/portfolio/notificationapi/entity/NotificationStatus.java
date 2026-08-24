package com.portfolio.notificationapi.entity;

/**
 * Represents the lifecycle status of a {@link Notification}.
 */
public enum NotificationStatus {
    PENDING,
    SCHEDULED,
    SENT,
    FAILED,
    CANCELLED
}
