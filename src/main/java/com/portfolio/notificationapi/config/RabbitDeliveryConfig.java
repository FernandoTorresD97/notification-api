package com.portfolio.notificationapi.config;
import org.springframework.amqp.core.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
@Configuration @ConditionalOnProperty(name="notification.delivery.enabled",havingValue="true")
public class RabbitDeliveryConfig {
    public static final String EXCHANGE="notifications.delivery";
    public static final String QUEUE="notifications.email";
    public static final String DLX="notifications.dead";
    public static final String DLQ="notifications.email.dead";
    @Bean public Declarables deliveryTopology() {
        DirectExchange exchange=new DirectExchange(EXCHANGE,true,false);
        DirectExchange dead=new DirectExchange(DLX,true,false);
        Queue queue=QueueBuilder.durable(QUEUE).deadLetterExchange(DLX).deadLetterRoutingKey("failed").build();
        Queue dlq=QueueBuilder.durable(DLQ).build();
        return new Declarables(exchange,dead,queue,dlq,
            BindingBuilder.bind(queue).to(exchange).with("email"),BindingBuilder.bind(dlq).to(dead).with("failed"));
    }
}
