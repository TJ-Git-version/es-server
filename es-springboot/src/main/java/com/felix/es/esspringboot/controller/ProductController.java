package com.felix.es.esspringboot.controller;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import com.felix.es.esspringboot.model.dto.ProductQueryDTO;
import com.felix.es.esspringboot.model.dto.vo.ProductListVO;
import com.felix.es.esspringboot.model.dto.vo.ProductVO;
import com.felix.es.esspringboot.model.es.Product;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    /**
     * 商品查询
     */
    @GetMapping("/query")
    public ProductListVO query(ProductQueryDTO productQueryDTO) throws IOException {
        ProductListVO productListVO = new ProductListVO();
        String keyword = productQueryDTO.getKeyword();
        //Map<String, Aggregation> aggregations = new HashMap<>();
        //if (productQueryDTO.getMinPrice() != null) {
        //    aggregations.put("min_price", Aggregation
        //            .of(a -> a
        //                    .min(MinAggregation.of(m -> m.field("price")))
        //            ));
        //}
        //if (productQueryDTO.getMaxPrice() != null) {
        //    aggregations.put("max_price", Aggregation
        //            .of(a -> a
        //                    .max(MaxAggregation.of(m -> m.field("price")))
        //            ));
        //}
        SearchResponse<Product> searchResponse = elasticsearchClient.search(s -> s
                        .index("product")
                        .from((productQueryDTO.getPageNo() - 1) * productQueryDTO.getPageSize())
                        .size(productQueryDTO.getPageSize())
                        .query(q -> q
                                .bool(b -> {
                                            if (productQueryDTO.getKeyword() != null && !productQueryDTO.getKeyword().isEmpty())
                                                b.must(m -> m
                                                        .match(ma -> ma
                                                                .field("title")
                                                                .query(keyword)
                                                        )
                                                );
                                            if (productQueryDTO.getBrand() != null && !productQueryDTO.getBrand().isEmpty())
                                                b.filter(m -> m
                                                        .term(t -> t
                                                                .field("brand.keyword")
                                                                .value(productQueryDTO.getBrand())
                                                        )
                                                );
                                            if (productQueryDTO.getMinPrice() != null || productQueryDTO.getMaxPrice() != null)
                                                b.filter(m -> m
                                                        .range(r -> {
                                                                    r.field("price");
                                                                    if (productQueryDTO.getMinPrice() != null) {
                                                                            r.gte(JsonData.of(productQueryDTO.getMinPrice()));
                                                                    }
                                                                    if (productQueryDTO.getMaxPrice() != null) {
                                                                            r.lte(JsonData.of(productQueryDTO.getMaxPrice()));
                                                                    }
                                                                    return r;
                                                                }
                                                        )
                                                );
                                            return b;
                                        }
                                )
                        )
                        //.aggregations(aggregations)
                        .highlight(h -> h
                                .preTags("<span style='color:red'>")
                                .postTags("</span>")
                                .fields("title", hf -> hf)
                        )
                , Product.class);
        productListVO.setTotal(elasticsearchClient.count(c -> c.index("product")).count());
        productListVO.setProductVOS(searchResponse.hits().hits()
                .stream()
                .filter(m -> m.source() != null)
                .map(m -> {
                    ProductVO productVO = new ProductVO();
                    BeanUtils.copyProperties(m.source(), productVO);
                    List<String> title = m.highlight().getOrDefault("title", List.of());
                    if (title.isEmpty()) {
                        productVO.setHighLightTitle(String.join("", title));
                    } else {
                        productVO.setHighLightTitle(m.source().getTitle());
                    }
                    return productVO;
                })
                .toList()
        );
        //Aggregate maxPrice = searchResponse.aggregations()
        //        .get("max_price");
        //long maxPriceValue = 0;
        //if (maxPrice != null) {
        //    maxPriceValue = maxPrice
        //            .sterms()
        //            .buckets()
        //            .array()
        //            .getFirst()
        //            .docCount();
        //}
        //long minPriceValue = 0;
        //Aggregate minPrice = searchResponse.aggregations()
        //        .get("min_price");
        //if (minPrice != null) {
        //    minPriceValue = minPrice
        //            .sterms()
        //            .buckets()
        //            .array()
        //            .getFirst()
        //            .docCount();
        //}
        //productListVO.setProductAggVO(ProductAggVO.builder()
        //        .maxPrice(maxPriceValue)
        //        .minPrice(minPriceValue)
        //        .build());
        return productListVO;
    }

}
