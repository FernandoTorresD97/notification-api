package com.portfolio.notificationapi.scheduler;
import com.portfolio.notificationapi.delivery.*;
import com.portfolio.notificationapi.config.DeliveryProperties;
import com.portfolio.notificationapi.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import java.time.*;
@Component @Slf4j @RequiredArgsConstructor
@ConditionalOnProperty(name="notification.delivery.enabled",havingValue="true")
public class NotificationDispatcher {
    private final NotificationRepository notifications;
    private final DeliveryPublisher publisher;
    private final DeliveryWorker worker;
    private final DeliveryProperties properties;
    private final Clock clock;
    @Scheduled(fixedDelayString="${notification.scheduler.fixed-delay-ms:5000}")
    public void dispatchDueNotifications(){
        var page=PageRequest.of(0,properties.getBatchSize());
        // The database remains the durable source of work. A broker outage
        // never marks a notification sent or removes it from future scans.
        for(Long id:notifications.findReadyIds(DeliveryWorker.READY,LocalDateTime.now(clock),page)){
            try{publisher.publish(id);}catch(RuntimeException ex){log.warn("notification={} publication deferred ({})",id,ex.getClass().getSimpleName());break;}
        }
        for(var n:notifications.findByDeadLetterPendingTrue(page)){
            try{publisher.deadLetter(n.getId());worker.acknowledgeDeadLetter(n.getId());}
            catch(RuntimeException ex){log.warn("notification={} dead-letter publication deferred",n.getId());break;}
        }
    }
}
