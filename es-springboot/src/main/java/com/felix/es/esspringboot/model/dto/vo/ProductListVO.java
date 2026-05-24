package com.felix.es.esspringboot.model.dto.vo;

import lombok.Data;

import java.util.List;

@Data
public class ProductListVO {

    private Long total;

    private List<ProductVO> productVOS;

    //private ProductAggVO productAggVO;

}
