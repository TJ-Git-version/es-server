package com.felix.esmysqlsync.service.impl;

import cn.hutool.core.bean.BeanUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.felix.esmysqlsync.mapper.ProductMapper;
import com.felix.esmysqlsync.model.bo.ProductEsBO;
import com.felix.esmysqlsync.model.constant.RabbitMQConstants;
import com.felix.esmysqlsync.model.entity.ProductEntity;
import com.felix.esmysqlsync.model.enums.OperationType;
import com.felix.esmysqlsync.service.ProductService;
import com.felix.esmysqlsync.utils.RabbitMQUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl extends ServiceImpl<ProductMapper, ProductEntity> implements ProductService {

    private static final int BATCH_SIZE = 1000;

    private final ElasticsearchClient elasticsearchClient;

    private final ThreadPoolTaskExecutor esThreadPoolTaskExecutor;

    private final RabbitMQUtil rabbitMQUtil;

    @Override
    public void createIndex() {
        try {
            elasticsearchClient.indices().delete(dr -> dr.index("product"));
            elasticsearchClient.indices().create(cir -> cir.index("product"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initIndexData() {
//        long count = count();
//        long pages = count / BATCH_SIZE + (count % BATCH_SIZE == 0 ? 0 : 1);
//        long lastId = 0;
//        for (int i = 0; i < pages; i++) {
//            List<ProductEntity> list = lambdaQuery().gt(ProductEntity::getId, lastId).orderByAsc(ProductEntity::getId).last("limit " + BATCH_SIZE).list();
//            if (list.isEmpty()) {
//                break;
//            }
//            lastId = list.getLast().getId();
//            List<BulkOperation> bulkOperations = list
//                    .stream()
//                    .map(m -> BulkOperation
//                            .of(bo -> bo
//                                    .create(co -> co
//                                            .id(m.getId().toString()).document(m))
//                            )
//                    ).toList();
//            try {
//                BulkResponse bulkResponse = elasticsearchClient.bulk(bk -> bk
//                        .index("product")
//                        .operations(bulkOperations)
//                );
//            } catch (IOException ignored) {
//            }
//            if (list.size() < BATCH_SIZE) {
//                break;
//            }
//        }
        long startTime = System.currentTimeMillis();
        // 允许积压的批次数，防止内存溢出
        final int maxPendingBatches = 8;
        Semaphore semaphore = new Semaphore(maxPendingBatches, true);
        List<Future<?>> futures = new ArrayList<>();
        long lastId = 0;
        try {
            while (true) {
                // 1. 查询一批数据（主线程）
                List<ProductEntity> list = lambdaQuery().gt(ProductEntity::getId, lastId).orderByAsc(ProductEntity::getId).last("limit " + BATCH_SIZE).list();
                if (list.isEmpty()) {
                    break;
                }
                lastId = list.getLast().getId();

                // 2. 控制并发：如果积压任务已达上限，阻塞直到有空位
                semaphore.acquire();

                long finalLastId = lastId;
                Future<?> future = esThreadPoolTaskExecutor.submit(() -> {
                    try {
                        List<BulkOperation> bulkOperations = list.stream()
                                .map(m -> BulkOperation.of(bo -> bo
                                        .create(co -> co
                                                .id(m.getId().toString())
                                                .document(m))))
                                .toList();
                        BulkResponse response = elasticsearchClient.bulk(BulkRequest.of(br -> br
                                .index("product")
                                .operations(bulkOperations)
                        ));
                        if (response.errors()) {
                            log.error("ES bulk response error batch ending id: {}", finalLastId);
                        } else {
                            log.info("ES bulk response success batch ending id: {}", finalLastId);
                        }
                    } catch (IOException e) {
                        log.error("ES bulk write failed for batch ending id: {}", finalLastId, e);
                    } finally {
                        // 释放信号量，允许新任务提交
                        semaphore.release();
                    }
                });
                futures.add(future);
                if (list.size() < BATCH_SIZE) {
                    break;
                }
            }
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (ExecutionException e) {
                    log.error("Task execution failed", e);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Data initialization interrupted", e);
        }
        log.info("Data initialization completed in {} ms", System.currentTimeMillis() - startTime);
    }

    @Override
    public boolean updateProduct(Long id, ProductEntity product) {
        product.setId(id);
        boolean updateFlag = updateById(product);
        if (updateFlag) {
            ProductEsBO productEsBO = BeanUtil.copyProperties(product, ProductEsBO.class);
            productEsBO.setUpdate();
            rabbitMQUtil.sendMessage(RabbitMQConstants.Product.EXCHANGE, RabbitMQConstants.Product.ROUTING_KEY, productEsBO);
        }
        return updateFlag;
    }

    @Override
    public boolean deleteProduct(Long id) {
        boolean removeFlag = removeById(id);
        if (removeFlag) {
            rabbitMQUtil.sendMessage(RabbitMQConstants.Product.EXCHANGE, RabbitMQConstants.Product.ROUTING_KEY, ProductEsBO.builder().id(id).operationType(OperationType.DELETE).build());
        }
        return removeFlag;
    }

    @Override
    public boolean addProduct(ProductEntity product) {
        boolean saveFlag = save(product);
        if (saveFlag) {
            ProductEsBO productEsBO = BeanUtil.copyProperties(product, ProductEsBO.class);
            productEsBO.setInsert();
            rabbitMQUtil.sendMessage(RabbitMQConstants.Product.EXCHANGE, RabbitMQConstants.Product.ROUTING_KEY, productEsBO);
        }
        return saveFlag;
    }
}
