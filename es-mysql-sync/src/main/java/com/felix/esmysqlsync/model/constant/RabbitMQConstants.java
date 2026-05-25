package com.felix.esmysqlsync.model.constant;

public class RabbitMQConstants {

    /**
     * 商品相关队列
     */
    public static final class Product {
        // 普通队列
        public static final String QUEUE = "product.queue";
        public static final String EXCHANGE = "product.exchange";
        public static final String ROUTING_KEY = "product.routing.key";

        // 死信队列
        public static final String DEAD_QUEUE = "product.dead.queue";
        public static final String DEAD_EXCHANGE = "product.dead.exchange";
        public static final String DEAD_ROUTING_KEY = "product.dead.routing.key";
    }



}
