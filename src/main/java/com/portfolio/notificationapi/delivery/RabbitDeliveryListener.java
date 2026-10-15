package com.portfolio.notificationapi.delivery;
import com.portfolio.notificationapi.config.RabbitDeliveryConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor @ConditionalOnProperty(name="notification.delivery.enabled",havingValue="true")
public class RabbitDeliveryListener {
    private final DeliveryWorker worker;
    @RabbitListener(queues=RabbitDeliveryConfig.QUEUE)
    public void consume(String body){
        final Long id;
        try{id=Long.valueOf(body);if(id<=0) throw new NumberFormatException();}
        catch(NumberFormatException ex){throw new AmqpRejectAndDontRequeueException("Invalid notification id");}
        worker.process(id);
    }
}
