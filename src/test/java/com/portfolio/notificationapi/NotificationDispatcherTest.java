package com.portfolio.notificationapi;
import com.portfolio.notificationapi.config.DeliveryProperties;
import com.portfolio.notificationapi.delivery.*;
import com.portfolio.notificationapi.entity.*;
import com.portfolio.notificationapi.repository.NotificationRepository;
import com.portfolio.notificationapi.scheduler.NotificationDispatcher;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;
class NotificationDispatcherTest {
    final NotificationRepository repository=mock(NotificationRepository.class);
    final DeliveryPublisher publisher=mock(DeliveryPublisher.class);
    final DeliveryWorker worker=mock(DeliveryWorker.class);
    final NotificationDispatcher dispatcher=new NotificationDispatcher(repository,publisher,worker,new DeliveryProperties(),Clock.systemUTC());
    @Test void brokerOutageDoesNotDropDatabaseWork(){
        when(repository.findReadyIds(anyList(),any(),any())).thenReturn(List.of(1L,2L));
        doThrow(new IllegalStateException("offline")).when(publisher).publish(1L);
        when(repository.findByDeadLetterPendingTrue(any())).thenReturn(List.of());
        dispatcher.dispatchDueNotifications();verify(publisher).publish(1L);verify(publisher,never()).publish(2L);verify(repository,never()).save(any());
    }
    @Test void deadLetterFlagIsOnlyAcknowledgedAfterPublish(){
        when(repository.findReadyIds(anyList(),any(),any())).thenReturn(List.of());
        when(repository.findByDeadLetterPendingTrue(any())).thenReturn(List.of(Notification.builder().id(1L).build()));
        doThrow(new IllegalStateException("offline")).when(publisher).deadLetter(1L);
        dispatcher.dispatchDueNotifications();verify(worker,never()).acknowledgeDeadLetter(anyLong());
    }
    @Test void confirmedDeadLetterPublicationClearsFlag(){
        when(repository.findReadyIds(anyList(),any(),any())).thenReturn(List.of());
        when(repository.findByDeadLetterPendingTrue(any())).thenReturn(List.of(Notification.builder().id(1L).build()));
        dispatcher.dispatchDueNotifications();var order=inOrder(publisher,worker);order.verify(publisher).deadLetter(1L);order.verify(worker).acknowledgeDeadLetter(1L);
    }
    @Test void exponentialBackoffIsCapped(){var p=new DeliveryProperties();assertThat(p.backoffSeconds(1)).isEqualTo(5);assertThat(p.backoffSeconds(2)).isEqualTo(10);assertThat(p.backoffSeconds(10)).isEqualTo(300);}
}
