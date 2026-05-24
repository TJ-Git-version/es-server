package com.felix.es.esspringboot.model.dto.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductAggVO {

    private Long minPrice;

    private Long maxPrice;

}
