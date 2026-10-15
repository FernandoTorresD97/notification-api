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
    private final com.portfolio.notificationapi.repository.DeliveryAttemptRepository attempts;

    @Transactional
    public NotificationResponse create(NotificationRequest request) {
        return create(request, null);
    }

    @Transactional
    public NotificationResponse create(NotificationRequest request, String key) {
        LocalDateTime scheduledAt = request.scheduledAt() == null ? null
                : request.scheduledAt().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        if (key != null) {
            if (key.isBlank() || key.length() > 120) throw new BusinessException("Idempotency-Key must contain 1 to 120 characters");
            var existing = notificationRepository.findByIdempotencyKey(key);
            if (existing.isPresent()) {
                Notification n = existing.get();
                if (!java.util.Objects.equals(n.getTitle(),request.title()) || !java.util.Objects.equals(n.getMessage(),request.message())
                    || !java.util.Objects.equals(n.getRecipient(),request.recipient()) || !java.util.Objects.equals(n.getChannel().getId(),request.channelId())
                    || !java.util.Objects.equals(n.getScheduledAt(),scheduledAt)) throw new BusinessException("Idempotency-Key was already used for a different request");
                return mapper.toResponse(n);
            }
        }
        Channel channel = channelService.getChannelOrThrow(request.channelId());

        if (!channel.isActive()) {
            throw new BusinessException("Channel '" + channel.getName() + "' is not active");
        }

        if (!"EMAIL".equalsIgnoreCase(channel.getName())) throw new BusinessException("Only EMAIL delivery is implemented");
        Notification notification = Notification.builder()
                .idempotencyKey(key)
                .title(request.title())
                .message(request.message())
                .recipient(request.recipient())
                .channel(channel)
                .scheduledAt(scheduledAt)
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
        Notification notification = notificationRepository.lockById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id " + id));

        if (notification.getStatus() == NotificationStatus.SENT) {
            throw new BusinessException("A notification that was already sent cannot be cancelled");
        }

        notification.setStatus(NotificationStatus.CANCELLED);
        notification.setNextAttemptAt(null);
        notification.setDeadLetterPending(false);
        return mapper.toResponse(notification);
    }

    @Transactional
    public void delete(Long id) {
        Notification n = notificationRepository.lockById(id).orElseThrow(() -> new ResourceNotFoundException("Notification not found with id " + id));
        attempts.deleteByNotification_Id(id);
        notificationRepository.delete(n);
    }

    @Transactional(readOnly = true)
    public java.util.List<com.portfolio.notificationapi.dto.DeliveryAttemptResponse> history(Long id) {
        getNotificationOrThrow(id);
        return attempts.findByNotification_IdOrderByAttemptNumberAsc(id).stream()
            .map(a -> new com.portfolio.notificationapi.dto.DeliveryAttemptResponse(a.getAttemptNumber(),a.getOutcome(),a.getErrorCode(),a.getAttemptedAt())).toList();
    }

    private Notification getNotificationOrThrow(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id " + id));
    }
}
