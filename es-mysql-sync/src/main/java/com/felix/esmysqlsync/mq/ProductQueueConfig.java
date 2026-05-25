package com.felix.esmysqlsync.mq;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.felix.esmysqlsync.model.constant.RabbitMQConstants.Product.*;

@Configuration
public class ProductQueueConfig {

    @Bean
    public Queue productQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public DirectExchange productExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public Binding productBinding(Queue productQueue, DirectExchange productExchange) {
        return BindingBuilder.bind(productQueue).to(productExchange).with(ROUTING_KEY);
    }

    // 死信队列
    @Bean
    public Queue productDlqQueue() {
        return new Queue(DEAD_QUEUE, true);
    }

    @Bean
    public DirectExchange productDlqExchange() {
        return new DirectExchange(DEAD_EXCHANGE, true, false);
    }

    @Bean
    public Binding productDlqBinding(Queue productDlqQueue, DirectExchange productDlqExchange) {
        return BindingBuilder.bind(productDlqQueue).to(productDlqExchange).with(DEAD_ROUTING_KEY);
    }

    // 队列绑定死信队列

    @Bean
    public Queue productBusinessQueue() {
        return QueueBuilder.durable()
                .deadLetterExchange(DEAD_EXCHANGE)
                .deadLetterRoutingKey(DEAD_ROUTING_KEY)
                .build();
    }
}
