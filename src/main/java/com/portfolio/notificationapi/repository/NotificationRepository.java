package com.portfolio.notificationapi.repository;

import com.portfolio.notificationapi.entity.Notification;
import com.portfolio.notificationapi.entity.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    java.util.Optional<Notification> findByIdempotencyKey(String key);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select n from Notification n join fetch n.channel where n.id = :id")
    java.util.Optional<Notification> lockById(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Query("select n.id from Notification n where n.status in :statuses and (n.scheduledAt is null or n.scheduledAt <= :now) and (n.nextAttemptAt is null or n.nextAttemptAt <= :now) order by n.createdAt")
    List<Long> findReadyIds(@org.springframework.data.repository.query.Param("statuses") List<NotificationStatus> statuses,
        @org.springframework.data.repository.query.Param("now") LocalDateTime now, Pageable pageable);
    List<Notification> findByDeadLetterPendingTrue(Pageable pageable);

    Page<Notification> findByStatus(NotificationStatus status, Pageable pageable);

    Page<Notification> findByChannel_Id(Long channelId, Pageable pageable);

    List<Notification> findByStatusAndScheduledAtLessThanEqual(
            NotificationStatus status, LocalDateTime now);
}
