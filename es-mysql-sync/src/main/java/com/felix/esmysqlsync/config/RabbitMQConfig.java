package com.felix.esmysqlsync.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RabbitMQConfig {

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);

        // 消息确认回调 - 确认消息是否到达交换机
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.info("消息已确认到达交换机: correlationData={}", correlationData);
            } else {
                log.error("消息未到达交换机: correlationData={}, cause={}", correlationData, cause);
            }
        });

        // 消息返回回调 - 确认消息是否正确路由到队列
        rabbitTemplate.setReturnsCallback(returned -> {
            log.error("消息未正确路由到队列: returned={}, message={}", returned, returned.getMessage());
        });

        return rabbitTemplate;
    }
}