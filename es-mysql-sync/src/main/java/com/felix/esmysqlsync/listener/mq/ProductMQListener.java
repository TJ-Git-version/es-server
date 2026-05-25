package com.felix.esmysqlsync.listener.mq;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import com.felix.esmysqlsync.mapper.ProductMapper;
import com.felix.esmysqlsync.model.bo.ProductEsBO;
import com.felix.esmysqlsync.model.constant.RabbitMQConstants;
import com.felix.esmysqlsync.model.constant.RedisConstants;
import com.felix.esmysqlsync.model.entity.ProductEntity;
import com.felix.esmysqlsync.model.vo.ProductEsVO;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductMQListener {

    private final ElasticsearchClient elasticsearchClient;
    private final ProductMapper productMapper;

    private final RedisTemplate<String, Object> redisTemplate;

    @RabbitListener(queues = RabbitMQConstants.Product.QUEUE)
    public void handleProduct(ProductEsBO productEsBO, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        String productRedisKey = StrUtil.format(RedisConstants.Product.QUEUE_REPEAT_KEY, productEsBO.getId());
        try {
            log.info("Received message data：{}", productEsBO);
            Boolean isFirst = redisTemplate.opsForValue().setIfAbsent(productRedisKey, productEsBO, Duration.ofHours(24));
            if (Boolean.FALSE.equals(isFirst)) {
                log.info("Message data already processed, acking message data：{}", productEsBO);
                // 已经处理过，直接确认
                channel.basicAck(deliveryTag, false);
                return;
            }
            process(productEsBO, channel, deliveryTag);
        } catch (Exception e) {
            try {
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException ex) {
                log.error("Failed to nack message data：{}", productEsBO, ex);
            }
        }finally {
            redisTemplate.delete(productRedisKey);
        }

    }

    private void process(ProductEsBO productEsBO, Channel channel, long deliveryTag) throws IOException {
        switch (productEsBO.getOperationType()) {
            case INSERT -> {
                if (checkExists(productEsBO)) {
                    log.info("Document already exists, no need to insert");
                    channel.basicAck(deliveryTag, false);
                }
                ProductEntity productEntity = productMapper.selectById(productEsBO.getId());
                elasticsearchClient.index(i -> i
                        .index("product")
                        .id(productEsBO.getId().toString())
                        .document(BeanUtil.copyProperties(productEntity, ProductEsVO.class))
                );
            }
            case UPDATE -> {
                if (!checkExists(productEsBO)) {
                    log.info("Document not exists, no need to update");
                    channel.basicAck(deliveryTag, false);
                }
                ProductEntity productEntity = productMapper.selectById(productEsBO.getId());
                elasticsearchClient.update(u -> u
                                .index("product")
                                .id(productEsBO.getId().toString())
                                .doc(BeanUtil.copyProperties(productEntity, ProductEsVO.class))
                        , ProductEsVO.class);
            }
            case DELETE -> {
                if (!checkExists(productEsBO)) {
                    log.info("Document not exists, no need to delete");
                    channel.basicAck(deliveryTag, false);
                }
                elasticsearchClient.delete(d -> d
                        .index("product")
                        .id(productEsBO.getId().toString())
                );
            }
        }
    }


    private boolean checkExists(ProductEsBO productEsBO) throws IOException {
        BooleanResponse existsResp = elasticsearchClient.exists(e -> e
                .index("product")
                .id(productEsBO.getId().toString())
        );
        log.info("Exists response: {}", existsResp.value());
        return existsResp.value();
    }


}
