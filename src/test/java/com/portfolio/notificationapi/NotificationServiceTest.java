package com.portfolio.notificationapi;

import com.portfolio.notificationapi.dto.NotificationRequest;
import com.portfolio.notificationapi.entity.Channel;
import com.portfolio.notificationapi.exception.BusinessException;
import com.portfolio.notificationapi.exception.ResourceNotFoundException;
import com.portfolio.notificationapi.mapper.NotificationMapper;
import com.portfolio.notificationapi.repository.NotificationRepository;
import com.portfolio.notificationapi.service.ChannelService;
import com.portfolio.notificationapi.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ChannelService channelService;

    @Spy
    private NotificationMapper mapper = new NotificationMapper();

    @InjectMocks
    private NotificationService notificationService;

    private Channel activeChannel;

    @BeforeEach
    void setUp() {
        activeChannel = Channel.builder().id(1L).name("EMAIL").active(true).build();
    }

    @Test
    void shouldCreateNotificationWithPendingStatusWhenNoScheduleGiven() {
        when(channelService.getChannelOrThrow(1L)).thenReturn(activeChannel);
        when(notificationRepository.save(any())).thenAnswer(invocation -> {
            var n = invocation.getArgument(0, com.portfolio.notificationapi.entity.Notification.class);
            n.setId(10L);
            n.setCreatedAt(java.time.LocalDateTime.now());
            return n;
        });

        NotificationRequest request = new NotificationRequest(
                "Welcome", "Hello there!", "user@example.com", 1L, null);

        var response = notificationService.create(request);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.status().name()).isEqualTo("PENDING");
    }

    @Test
    void shouldRejectNotificationWhenChannelIsInactive() {
        Channel inactive = Channel.builder().id(2L).name("SMS").active(false).build();
        when(channelService.getChannelOrThrow(2L)).thenReturn(inactive);

        NotificationRequest request = new NotificationRequest(
                "Alert", "Something happened", "+351123456789", 2L, null);

        assertThatThrownBy(() -> notificationService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void shouldThrowWhenNotificationNotFound() {
        when(notificationRepository.findById(99L)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> notificationService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
