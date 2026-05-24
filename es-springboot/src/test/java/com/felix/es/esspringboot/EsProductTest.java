package com.felix.es.esspringboot;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.json.JsonData;
import com.felix.es.esspringboot.model.es.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
public class EsProductTest {

    @Autowired
    private ElasticsearchClient client;

    /**
     * 创建索引
     */
    @Test
    public void createIndex() throws IOException {
        CreateIndexResponse productResp = client.indices().create(c -> c.index("product"));
        System.out.println(productResp);
    }

    /**
     * 插入文档
     */
    @Test
    public void insertDocument() throws IOException {
        //Product product = new Product();
        //product.setId(1L);
        //product.setTitle("华为 Mate60 Pro");
        //product.setBrand("华为");
        //product.setPrice(6999D);
        //product.setStock(100);
        Product product = new Product();
        product.setId(2L);
        product.setTitle("苹果 iPhone 17");
        product.setBrand("苹果");
        product.setPrice(7999D);
        product.setStock(200);
        IndexResponse indexResponse = client.index(
                new IndexRequest.Builder<>()
                        .index("product")
                        .id(product.getId().toString())
                        .document(product)
                        .build());
        System.out.println(indexResponse);
        //client.index(i ->
        //        i.index("product").id(product.getId().toString()).document(product)
        //);
    }

    /**
     * 查询文档
     */
    @Test
    public void queryDocument() throws IOException {
        GetResponse<Product> productGetResponse = client.get(g -> g.index("product").id("1"), Product.class);
        System.out.println(productGetResponse);
        Product product = productGetResponse.source();
        System.out.println(product);
    }

    /**
     * 删除文档
     */
    @Test
    public void deleteDocument() throws IOException {
        DeleteResponse deleteResponse = client.delete(d -> d.index("product").id("1"));
        System.out.println(deleteResponse);
    }

    /**
     * match查询
     */
    @Test
    public void matchQuery() throws IOException {
        SearchResponse<Product> searchResponse = client.search(r ->
                r.index("product")
                        .query(q ->
                                q.match(m ->
                                        m.field("title").query("华为")
                                )
                        ), Product.class);
        System.out.println(searchResponse);
        for (Hit<Product> hit : searchResponse.hits().hits()) {
            System.out.println(hit.source());
        }
    }

    /**
     * boolQuery查询
     */
    @Test
    public void boolQuery() throws IOException {
        SearchResponse<Product> searchResponse = client.search(s -> s
                .index("product")
                .query(q -> q
                        .bool(b -> b
                                .must(m -> m
                                        .match(mm -> mm
                                                .field("title")
                                                .query("Pro"))
                                )
                                .filter(f -> f
                                        .term(t -> t
                                                .field("brand.keyword")
                                                .value("华为")
                                        )
                                )
                        )
                ), Product.class);
        System.out.println(searchResponse);
        for (Hit<Product> hit : searchResponse.hits().hits()) {
            System.out.println(hit.source());
        }
    }

    /**
     * 范围查询
     */
    @Test
    public void rangeQuery() throws IOException {
        SearchResponse<Product> searchResponse = client.search(s -> s
                        .index("product")
                        .query(q -> q
                                .range(qr -> qr
                                        .field("price")
                                        .gte(JsonData.of(5000))
                                        .lte(JsonData.of(10000))
                                )
                        )
                , Product.class);
        System.out.println(searchResponse);
        for (Hit<Product> hit : searchResponse.hits().hits()) {
            System.out.println(hit.source());
        }
    }

    /**
     * 分页查询
     */
    @Test
    public void pageQuery() throws IOException {
        SearchResponse<Product> searchResponse = client.search(s -> s
                        .index("product")
                        .from(0)
                        .size(10)
                , Product.class);
        System.out.println(searchResponse);
        for (Hit<Product> hit : searchResponse.hits().hits()) {
            System.out.println(hit.source());
        }
    }

    /**
     * 排序查询
     */
    @Test
    public void sortQuery() throws IOException {
        SearchResponse<Product> searchResponse = client.search(s -> s
                        .index("product")
                        .sort(sort ->
                                sort.field(f -> f
                                        .field("price")
                                        .order(SortOrder.Asc))
                        )
                , Product.class);
        System.out.println(searchResponse);
        for (Hit<Product> hit : searchResponse.hits().hits()) {
            System.out.println(hit.source());
        }
    }

    /**
     * 高亮搜索
     */
    @Test
    public void highlightQuery() throws IOException {
        SearchResponse<Product> searchResponse = client.search(s -> s
                        .index("product")
                        .query(q -> q
                                .match(m -> m
                                        .field("title").query("华为")
                                )
                        )
                        .highlight(h -> h
                                .fields("title", hf -> hf)
                                .preTags("<span style='color:red'>")
                                .postTags("</span>")
                        )
                , Product.class);
        System.out.println(searchResponse);
        for (Hit<Product> hit : searchResponse.hits().hits()) {
            System.out.println(hit.source());
            System.out.println(hit.highlight());
        }
    }

    /**
     * 聚合查询
     */
    @Test
    public void aggregateQuery() throws IOException {
        SearchResponse<Void> response = client.search(s -> s
                        .index("product")
                        .size(0)
                        .aggregations("brandAgg", a -> a
                                .terms(t -> t
                                        .field("brand.keyword")
                                )
                        )
                , Void.class);
        System.out.println(response);
        System.out.println(response.aggregations().get("brandAgg"));
        StringTermsAggregate brandAgg = response.aggregations().get("brandAgg").sterms();
        for (StringTermsBucket bucket : brandAgg.buckets().array()) {
            System.out.println(bucket.key().stringValue() + ":" + bucket.docCount());

        }

    }
}
