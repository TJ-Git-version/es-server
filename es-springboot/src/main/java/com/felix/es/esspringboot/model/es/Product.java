package com.felix.es.esspringboot.model.es;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Product {
    private Long id;

    private String title;

    private String brand;

    private Double price;

    private Integer stock;
}
