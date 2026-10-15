package com.portfolio.notificationapi.delivery;
public interface DeliveryPublisher {
    void publish(Long notificationId);
    void deadLetter(Long notificationId);
}
