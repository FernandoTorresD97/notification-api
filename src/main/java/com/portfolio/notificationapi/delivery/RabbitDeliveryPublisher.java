package com.portfolio.notificationapi.delivery;
import com.portfolio.notificationapi.config.RabbitDeliveryConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
@Component @RequiredArgsConstructor @ConditionalOnProperty(name="notification.delivery.enabled",havingValue="true")
public class RabbitDeliveryPublisher implements DeliveryPublisher {
    private final RabbitTemplate rabbit;
    public void publish(Long id){send(id,RabbitDeliveryConfig.EXCHANGE,"email");}
    public void deadLetter(Long id){send(id,RabbitDeliveryConfig.DLX,"failed");}
    private void send(Long id,String exchange,String route){
        MessageProperties props=new MessageProperties();
        props.setContentType("text/plain"); props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        props.setMessageId("notification-"+id);
        CorrelationData confirmation=new CorrelationData(UUID.randomUUID().toString());
        rabbit.setMandatory(true);
        rabbit.send(exchange,route,new Message(id.toString().getBytes(StandardCharsets.UTF_8),props),confirmation);
        try {
            if(!confirmation.getFuture().get(5,TimeUnit.SECONDS).isAck() || confirmation.getReturned()!=null)
                throw new IllegalStateException("Broker did not confirm a routed message");
        }catch(InterruptedException ex){Thread.currentThread().interrupt();throw new IllegalStateException("Broker confirmation interrupted",ex);}
        catch(java.util.concurrent.ExecutionException|java.util.concurrent.TimeoutException ex){throw new IllegalStateException("Broker confirmation unavailable",ex);}
    }
}
