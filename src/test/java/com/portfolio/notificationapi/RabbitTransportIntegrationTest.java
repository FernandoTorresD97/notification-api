package com.portfolio.notificationapi;
import com.portfolio.notificationapi.config.RabbitDeliveryConfig;
import com.portfolio.notificationapi.delivery.*;
import org.junit.jupiter.api.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.*;
import org.springframework.amqp.core.*;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.*;
import static org.assertj.core.api.Assertions.*;
@Testcontainers(disabledWithoutDocker=true)
class RabbitTransportIntegrationTest {
    @Container static RabbitMQContainer broker=new RabbitMQContainer("rabbitmq:3.13-management-alpine");
    static CachingConnectionFactory connection;
    RabbitTemplate rabbit;RabbitDeliveryPublisher publisher;
    @BeforeAll static void connect(){connection=new CachingConnectionFactory(broker.getHost(),broker.getAmqpPort());connection.setUsername(broker.getAdminUsername());connection.setPassword(broker.getAdminPassword());connection.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);connection.setPublisherReturns(true);}
    @AfterAll static void close(){connection.destroy();}
    @BeforeEach void setup(){
        rabbit=new RabbitTemplate(connection);var admin=new RabbitAdmin(connection);
        var topology=new RabbitDeliveryConfig().deliveryTopology();
        for(var d:topology.getDeclarables()){
            if(d instanceof Exchange e)admin.declareExchange(e);
            if(d instanceof Queue q)admin.declareQueue(q);
            if(d instanceof Binding b)admin.declareBinding(b);
        }
        admin.purgeQueue(RabbitDeliveryConfig.QUEUE);admin.purgeQueue(RabbitDeliveryConfig.DLQ);
        publisher=new RabbitDeliveryPublisher(rabbit);
    }
    @Test void publisherConfirmsPersistentDeliveryWithStableId(){
        publisher.publish(42L);var message=rabbit.receive(RabbitDeliveryConfig.QUEUE,5000);
        assertThat(message).isNotNull();assertThat(new String(message.getBody(),java.nio.charset.StandardCharsets.UTF_8)).isEqualTo("42");
        assertThat(message.getMessageProperties().getReceivedDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
        assertThat(message.getMessageProperties().getMessageId()).isEqualTo("notification-42");
    }
    @Test void failedNotificationIsRoutedToDurableDeadLetterQueue(){publisher.deadLetter(45L);assertThat(rabbit.receive(RabbitDeliveryConfig.DLQ,5000)).isNotNull();}
    @Test void unroutableMessageIsNotReportedAsPublished(){new RabbitAdmin(connection).deleteQueue(RabbitDeliveryConfig.QUEUE);assertThatThrownBy(()->publisher.publish(42L)).isInstanceOf(IllegalStateException.class);}
}
