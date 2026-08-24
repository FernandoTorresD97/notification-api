package com.portfolio.notificationapi.repository;

import com.portfolio.notificationapi.entity.Notification;
import com.portfolio.notificationapi.entity.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByStatus(NotificationStatus status, Pageable pageable);

    Page<Notification> findByChannel_Id(Long channelId, Pageable pageable);

    List<Notification> findByStatusAndScheduledAtLessThanEqual(
            NotificationStatus status, LocalDateTime now);
}
