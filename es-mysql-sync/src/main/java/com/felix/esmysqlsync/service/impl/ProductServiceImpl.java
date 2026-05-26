package com.felix.esmysqlsync.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.ReindexResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.reindex.Destination;
import co.elastic.clients.elasticsearch.core.reindex.Source;
import co.elastic.clients.elasticsearch.indices.UpdateAliasesResponse;
import co.elastic.clients.elasticsearch.indices.analyze.AnalyzeToken;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.felix.esmysqlsync.mapper.ProductMapper;
import com.felix.esmysqlsync.model.bo.ProductEsBO;
import com.felix.esmysqlsync.model.constant.RabbitMQConstants;
import com.felix.esmysqlsync.model.dto.ProductQueryDTO;
import com.felix.esmysqlsync.model.entity.ProductEntity;
import com.felix.esmysqlsync.model.enums.OperationType;
import com.felix.esmysqlsync.model.vo.IndexMappingVO;
import com.felix.esmysqlsync.model.vo.ProductEsVO;
import com.felix.esmysqlsync.model.vo.ProductListVO;
import com.felix.esmysqlsync.service.ProductService;
import com.felix.esmysqlsync.utils.RabbitMQUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl extends ServiceImpl<ProductMapper, ProductEntity> implements ProductService {

    private static final int BATCH_SIZE = 1000;

    private final ElasticsearchClient elasticsearchClient;

    private final ThreadPoolTaskExecutor esThreadPoolTaskExecutor;

    private final RabbitMQUtil rabbitMQUtil;

    private final TransactionTemplate transactionTemplate;

    /**
     * 全量同步数据从旧索引到新索引
     */
    @Override
    public Map<String, Object> reindexData(String sourceIndex, String destIndex) {
        Map<String, Object> result = new HashMap<>();
        result.put("sourceIndex", sourceIndex);
        result.put("destIndex", destIndex);
        try {
            ReindexResponse response = elasticsearchClient.reindex(r -> r
                    .source(Source.of(s -> s.index(sourceIndex)))
                    .dest(Destination.of(d -> d.index(destIndex)))
                    .refresh(true)
            );
            result.put("total", response.total());
            result.put("failures", response.failures().size());
            result.put("status", "completed");
            log.info("同步完成：成功 {} 条，失败 {} 条", response.total(), response.failures().size());
        } catch (IOException e) {
            result.put("status", "failed");
            result.put("error", e.getMessage());
            throw new RuntimeException(e);
        }
        return result;
    }

    /**
     * 异步全量同步（带进度跟踪）
     */
    @Override
    public String reindexDataAsync(String sourceIndex, String destIndex) {
        try {
            var response = elasticsearchClient.reindex(r -> r
                    .source(Source.of(s -> s.index(sourceIndex)))
                    .dest(Destination.of(d -> d.index(destIndex)))
                    .waitForCompletion(false)
                    .refresh(true)
            );
            return response.task();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取Reindex任务进度
     */
    @Override
    public Map<String, Object> getReindexProgress(String taskId) {
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("taskId", taskId);
            var taskInfo = elasticsearchClient.tasks().get(t -> t.taskId(taskId));
            result.put("type", taskInfo.task() != null ? taskInfo.task().type() : null);
            result.put("action", taskInfo.task() != null ? taskInfo.task().action() : null);
            result.put("status", taskInfo.task() != null ? taskInfo.task().status().toString() : null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    /**
     * 通用别名切换方法：自动获取别名当前指向的索引，切换到新索引
     * @param newIndex 新索引名
     * @param alias 公共别名
     */
    @Override
    public boolean switchIndexAliasAuto(String oldIndex, String newIndex, String alias)  {
        log.info("Switching index alias from {} to {} for alias {}", oldIndex, newIndex, alias);
        try {
            UpdateAliasesResponse updateAliasesResponse = elasticsearchClient.indices().updateAliases(uar -> uar
                    .actions(a -> {
                                if (oldIndex != null) {
                                    log.info("Removing alias {} from index {}", alias, oldIndex);
                                    a.remove(r -> r.index(oldIndex).alias(alias));
                                }
                                if (newIndex != null) {
                                    log.info("Adding alias {} to index {}", alias, newIndex);
                                    a.add(aa -> aa.index(newIndex).alias(alias).isWriteIndex(true));
                                }
                                return a;
                            }
                    )
            );
            return updateAliasesResponse.acknowledged();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void createIndex() {
        try {
            BooleanResponse exists = elasticsearchClient.indices().exists(er -> er.index("product_v2"));
            if (exists.value()) {
                log.info("Index product_v2 already exists");
                return;
            }
            Map<String, Property> properties = new HashMap<>();
            properties.put("productName", Property.of(p -> p
                            .text(t -> t
                                    .analyzer("ik_max_word")
                                    .searchAnalyzer("ik_smart")
                                    .fields("keyword", f -> f
                                            .keyword(k -> k
                                                    .ignoreAbove(256)
                                            )
                                    )
                            )
                    )
            );
            properties.put("productNo", Property.of(p -> p
                    .text(t -> t
                            .analyzer("ik_max_word")
                            .searchAnalyzer("ik_smart")
                            .fields("keyword", f -> f
                                    .keyword(k -> k
                                            .ignoreAbove(256)
                                    )
                            )
                    )
            ));
            properties.put("categoryId", Property.of(p -> p
                    .keyword(k -> k)
            ));
            properties.put("brandId", Property.of(p -> p
                    .keyword(k -> k)
            ));
            properties.put("price", Property.of(p -> p
                    .double_(d -> d)
            ));
            properties.put("marketPrice", Property.of(p -> p
                    .double_(d -> d)
            ));
            properties.put("costPrice", Property.of(p -> p
                    .double_(d -> d)
            ));
            properties.put("stock", Property.of(p -> p
                    .integer(i -> i)
            ));
            properties.put("status", Property.of(p -> p
                    .integer(i -> i)
            ));
            properties.put("sort", Property.of(p -> p
                    .integer(i -> i)
            ));
            properties.put("createTime", Property.of(p -> p
                    .date(d -> d
                            .format("yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis||epoch_second")
                    )
            ));
            properties.put("updateTime", Property.of(p -> p
                    .date(d -> d
                            .format("yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis||epoch_second")
                    )
            ));
            elasticsearchClient.indices().create(cir -> cir
                    .index("product_v2")
                    .mappings(m -> m
                            .properties(properties)
                    ));
        } catch (Exception e) {
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
                                        .index(io -> io
                                                .id(m.getId().toString())
                                                .document(m))))
                                .toList();
                        BulkResponse response = elasticsearchClient.bulk(BulkRequest.of(br -> br
                                .index("product_v1")
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
            elasticsearchClient.indices().refresh(r -> r.index("product_v1"));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Data initialization interrupted", e);
        } catch (IOException e) {
            log.error("Data initialization failed", e);
        }
        log.info("Data initialization completed in {} ms", System.currentTimeMillis() - startTime);
    }

    @Override
    public boolean updateProduct(Long id, ProductEntity product) {
        product.setId(id);
        Boolean updateFlag = transactionTemplate.execute((status) -> updateById(product));
        if (Boolean.TRUE.equals(updateFlag)) {
            ProductEsBO productEsBO = BeanUtil.copyProperties(product, ProductEsBO.class);
            productEsBO.setUpdate();
            rabbitMQUtil.sendMessage(RabbitMQConstants.Product.EXCHANGE, RabbitMQConstants.Product.ROUTING_KEY, productEsBO);
        }
        return Boolean.TRUE.equals(updateFlag);
    }

    @Override
    public boolean deleteProduct(Long id) {
        Boolean removeFlag = transactionTemplate.execute((status) -> removeById(id));
        if (Boolean.TRUE.equals(removeFlag)) {
            rabbitMQUtil.sendMessage(RabbitMQConstants.Product.EXCHANGE, RabbitMQConstants.Product.ROUTING_KEY, ProductEsBO.builder().id(id).operationType(OperationType.DELETE).build());
        }
        return Boolean.TRUE.equals(removeFlag);
    }

    @Override
    public boolean addProduct(ProductEntity product) {
        Boolean saveFlag = transactionTemplate.execute((status) -> save(product));
        if (Boolean.TRUE.equals(saveFlag)) {
            ProductEsBO productEsBO = BeanUtil.copyProperties(product, ProductEsBO.class);
            productEsBO.setInsert();
            rabbitMQUtil.sendMessage(RabbitMQConstants.Product.EXCHANGE, RabbitMQConstants.Product.ROUTING_KEY, productEsBO);
        }
        return Boolean.TRUE.equals(saveFlag);
    }

    @Override
    public ProductListVO listProductPage(ProductQueryDTO queryDTO) {
        log.info("query product list: {}", queryDTO);
        try {
            long count = elasticsearchClient.count().count();
            log.info("query product list count: {}", count);
            if (count == 0) {
                return ProductListVO.builder().total(0L).build();
            }
            SearchResponse<ProductEsVO> searchResponse = elasticsearchClient.search(sr -> sr
                            .index("product")
                            .query(qr -> qr
                                    .bool(bq -> {
                                                if (StrUtil.isNotBlank(queryDTO.getProductName())) {
                                                    bq.must(m -> m.match(mc -> mc
                                                            .field("productName").query(queryDTO.getProductName())
                                                    ));
                                                }
                                                if (StrUtil.isNotBlank(queryDTO.getProductNo())) {
                                                    bq.must(m -> m.match(mc -> mc
                                                            .field("productNo").query(queryDTO.getProductNo())
                                                    ));
                                                }
                                                Optional.ofNullable(queryDTO.getCategoryId()).ifPresent(categoryId -> {
                                                    bq.filter(q -> q.term(tq -> tq.field("categoryId").value(categoryId)));
                                                });
                                                Optional.ofNullable(queryDTO.getBrandId()).ifPresent(brandId -> {
                                                    bq.filter(q -> q.term(tq -> tq.field("brandId").value(brandId)));
                                                });
                                                Optional.ofNullable(queryDTO.getStatus()).ifPresent(status -> {
                                                    bq.filter(q -> q.term(tq -> tq.field("status").value(status)));
                                                });
                                                Optional.ofNullable(queryDTO.getHasStock()).ifPresent(hasStock -> {
                                                    bq.filter(q -> q.range(rq -> rq.field("stock").gt(JsonData.of(0))));
                                                });
                                                Optional.ofNullable(queryDTO.getMinPrice()).ifPresent(minPrice -> {
                                                    bq.filter(q -> q.range(rq -> rq.field("price").gte(JsonData.of(minPrice))));
                                                });
                                                Optional.ofNullable(queryDTO.getMaxPrice()).ifPresent(maxPrice -> {
                                                    bq.filter(q -> q.range(rq -> rq.field("price").lte(JsonData.of(maxPrice))));
                                                });
                                                Optional.ofNullable(queryDTO.getMinMarketPrice()).ifPresent(minMarketPrice -> {
                                                    bq.filter(q -> q.range(rq -> rq.field("marketPrice").gte(JsonData.of(minMarketPrice))));
                                                });
                                                Optional.ofNullable(queryDTO.getMaxMarketPrice()).ifPresent(maxMarketPrice -> {
                                                    bq.filter(q -> q.range(rq -> rq.field("marketPrice").lte(JsonData.of(maxMarketPrice))));
                                                });
                                                Optional.ofNullable(queryDTO.getMinCostPrice()).ifPresent(minCostPrice -> {
                                                    bq.filter(q -> q.range(rq -> rq.field("costPrice").gte(JsonData.of(minCostPrice))));
                                                });
                                                Optional.ofNullable(queryDTO.getMaxCostPrice()).ifPresent(maxCostPrice -> {
                                                    bq.filter(q -> q.range(rq -> rq.field("costPrice").lte(JsonData.of(maxCostPrice))));
                                                });
                                                Optional.ofNullable(queryDTO.getMinStock()).ifPresent(minStock -> {
                                                    bq.filter(q -> q.range(rq -> rq.field("stock").gte(JsonData.of(minStock))));
                                                });
                                                Optional.ofNullable(queryDTO.getMaxStock()).ifPresent(maxStock -> {
                                                    bq.filter(q -> q.range(rq -> rq.field("stock").lte(JsonData.of(maxStock))));
                                                });
                                                Optional.ofNullable(queryDTO.getCreateTimeStart()).ifPresent(createTimeStart -> {
                                                    bq.filter(q -> q.range(rq -> rq.field("createTime").gte(JsonData.of(createTimeStart))));
                                                });
                                                Optional.ofNullable(queryDTO.getCreateTimeEnd()).ifPresent(createTimeEnd -> {
                                                    bq.filter(q -> q.range(rq -> rq.field("createTime").lte(JsonData.of(createTimeEnd))));
                                                });
                                                Optional.ofNullable(queryDTO.getUpdateTimeStart()).ifPresent(updateTimeStart -> {
                                                    bq.filter(q -> q.range(rq -> rq.field("updateTime").gte(JsonData.of(updateTimeStart))));
                                                });
                                                Optional.ofNullable(queryDTO.getUpdateTimeEnd()).ifPresent(updateTimeEnd -> {
                                                    bq.filter(q -> q.range(rq -> rq.field("updateTime").lte(JsonData.of(updateTimeEnd))));
                                                });
                                                return bq;
                                            }
                                    )
                            )
                            .from((queryDTO.getPageNum() - 1) * queryDTO.getPageSize())
                            .size(queryDTO.getPageSize())
                            .highlight(hl -> hl
                                    .fields("productName", hf -> hf.preTags("<span style='color:red'>").postTags("</span>"))
                                    .fields("productNo", hf -> hf.preTags("<span style='color:red'>").postTags("</span>"))
                            )
                            .sort(so -> so
                                    .field(f -> f.field("createTime").order(SortOrder.Desc))
                            )
                    , ProductEsVO.class);
            log.info("query product list: {}", searchResponse);
            List<ProductEsVO> productEsVOS = searchResponse.hits().hits().stream().map(m -> {
                ProductEsVO productEsVO = BeanUtil.copyProperties(m.source(), ProductEsVO.class);
                productEsVO.setHighlightProductName(String.join("", m.highlight().getOrDefault("productName", List.of())));
                productEsVO.setHighlightProductNo(String.join("", m.highlight().getOrDefault("productNo", List.of())));
                return productEsVO;
            }).toList();
            return ProductListVO.builder().total(count).productEsVOList(productEsVOS).build();
        } catch (IOException e) {
            log.error("query product list error", e);
            throw new RuntimeException("query product list error");
        }
    }

    @Override
    public Map<String, Object> testSegment(String text) {
        log.info("test segment: {}", text);
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("ik_max_word", elasticsearchClient.indices().analyze(ar -> ar.analyzer("ik_max_word").text(text)).tokens().stream().map(AnalyzeToken::token).collect(Collectors.joining("、")));
            result.put("ik_smart", elasticsearchClient.indices().analyze(ar -> ar.analyzer("ik_smart").text(text)).tokens().stream().map(AnalyzeToken::token).collect(Collectors.joining("、")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    @Override
    public IndexMappingVO getMapping(String index) {
        log.info("get mapping: {}", index);
        try {
            var response = elasticsearchClient.indices().getMapping(gm -> gm.index(index));
            var indexMapping = response.result().get(index);
            if (indexMapping == null) {
                return null;
            }
            IndexMappingVO vo = new IndexMappingVO();
            vo.setIndexName(index);
            var properties = indexMapping.mappings().properties();
            List<IndexMappingVO.FieldMapping> fieldMappings = new ArrayList<>();
            properties.forEach((fieldName, property) -> {
                IndexMappingVO.FieldMapping fm = new IndexMappingVO.FieldMapping();
                fm.setFieldName(fieldName);
                fm.setType(property._kind().toString().toLowerCase());
                if (property.isText()) {
                    var textProp = property.text();
                    fm.setAnalyzer(textProp.analyzer());
                    fm.setSearchAnalyzer(textProp.searchAnalyzer());
                    if (textProp.fields() != null && textProp.fields().containsKey("keyword")) {
                        fm.setFields(Map.of("keyword", Map.of("type", "keyword", "ignore_above", 256)));
                    }
                } else if (property.isDate()) {
                    fm.setFormat(property.date().format());
                } else if (property.isKeyword()) {
                    var kw = property.keyword();
                    if (kw.ignoreAbove() != null) {
                        fm.setFields(Map.of("keyword", Map.of("type", "keyword", "ignore_above", kw.ignoreAbove())));
                    }
                }
                fieldMappings.add(fm);
            });
            vo.setProperties(fieldMappings);
            return vo;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
