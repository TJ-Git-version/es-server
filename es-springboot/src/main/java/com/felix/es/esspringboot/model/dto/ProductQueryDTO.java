package com.felix.es.esspringboot.model.dto;

import lombok.Data;

@Data
public class ProductQueryDTO {

    private String keyword;

    private String brand;

    private Integer minPrice;

    private Integer maxPrice;

    private Integer pageNo;

    private Integer pageSize;

}
