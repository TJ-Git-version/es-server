package com.felix.es.esspringboot.model.dto.vo;

import lombok.Data;

@Data
public class ProductVO {

    private Long id;

    private String title;

    private String highLightTitle;

    private String brand;

    private Double price;

    private Integer stock;

}
