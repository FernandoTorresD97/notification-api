package com.portfolio.notificationapi.service;

import com.portfolio.notificationapi.dto.NotificationRequest;
import com.portfolio.notificationapi.dto.NotificationResponse;
import com.portfolio.notificationapi.entity.Channel;
import com.portfolio.notificationapi.entity.Notification;
import com.portfolio.notificationapi.entity.NotificationStatus;
import com.portfolio.notificationapi.exception.BusinessException;
import com.portfolio.notificationapi.exception.ResourceNotFoundException;
import com.portfolio.notificationapi.mapper.NotificationMapper;
import com.portfolio.notificationapi.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ChannelService channelService;
    private final NotificationMapper mapper;

    @Transactional
    public NotificationResponse create(NotificationRequest request) {
        Channel channel = channelService.getChannelOrThrow(request.channelId());

        if (!channel.isActive()) {
            throw new BusinessException("Channel '" + channel.getName() + "' is not active");
        }

        Notification notification = Notification.builder()
                .title(request.title())
                .message(request.message())
                .recipient(request.recipient())
                .channel(channel)
                .scheduledAt(request.scheduledAt())
                .status(request.scheduledAt() != null
                        ? NotificationStatus.SCHEDULED
                        : NotificationStatus.PENDING)
                .build();

        return mapper.toResponse(notificationRepository.save(notification));
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> findAll(Pageable pageable) {
        return notificationRepository.findAll(pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> findByStatus(NotificationStatus status, Pageable pageable) {
        return notificationRepository.findByStatus(status, pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public NotificationResponse findById(Long id) {
        return mapper.toResponse(getNotificationOrThrow(id));
    }

    @Transactional
    public NotificationResponse cancel(Long id) {
        Notification notification = getNotificationOrThrow(id);

        if (notification.getStatus() == NotificationStatus.SENT) {
            throw new BusinessException("A notification that was already sent cannot be cancelled");
        }

        notification.setStatus(NotificationStatus.CANCELLED);
        return mapper.toResponse(notification);
    }

    @Transactional
    public void delete(Long id) {
        notificationRepository.delete(getNotificationOrThrow(id));
    }

    /**
     * Used by the scheduler to pick up notifications that are due for dispatch.
     */
    @Transactional
    public List<Notification> findDueNotifications() {
        return notificationRepository.findByStatusAndScheduledAtLessThanEqual(
                NotificationStatus.SCHEDULED, LocalDateTime.now());
    }

    @Transactional
    public void markAsSent(Notification notification) {
        notification.setStatus(NotificationStatus.SENT);
        notification.setSentAt(LocalDateTime.now());
    }

    @Transactional
    public void markAsFailed(Notification notification) {
        notification.setStatus(NotificationStatus.FAILED);
    }

    private Notification getNotificationOrThrow(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id " + id));
    }
}
