package com.portfolio.notificationapi.scheduler;

import com.portfolio.notificationapi.entity.Notification;
import com.portfolio.notificationapi.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Periodically looks for SCHEDULED notifications whose scheduledAt time has
 * passed and dispatches them. The actual delivery mechanism (email/SMS/push
 * provider integration) is intentionally abstracted here — in the original
 * project this simulated dispatch by logging; swap simulateDelivery() for a
 * real provider client to make this production-ready.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDispatcher {

    private final NotificationService notificationService;

    @Scheduled(fixedDelayString = "${notification.scheduler.fixed-delay-ms:30000}")
    public void dispatchDueNotifications() {
        List<Notification> due = notificationService.findDueNotifications();

        if (due.isEmpty()) {
            return;
        }

        log.info("Dispatching {} due notification(s)", due.size());

        for (Notification notification : due) {
            try {
                simulateDelivery(notification);
                notificationService.markAsSent(notification);
                log.info("Notification {} sent via {}", notification.getId(),
                        notification.getChannel().getName());
            } catch (Exception ex) {
                notificationService.markAsFailed(notification);
                log.error("Failed to send notification {}", notification.getId(), ex);
            }
        }
    }

    private void simulateDelivery(Notification notification) {
        log.debug("Sending '{}' to {} via {}", notification.getTitle(),
                notification.getRecipient(), notification.getChannel().getName());
    }
}
