package com.felix.esmysqlsync.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RabbitMQUtil {

    private final RabbitTemplate rabbitTemplate;

    // 发送消息
    public void sendMessage(String exchange, String routingKey, Object message) {
        // TODO: 实现发送消息的逻辑
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }

}
