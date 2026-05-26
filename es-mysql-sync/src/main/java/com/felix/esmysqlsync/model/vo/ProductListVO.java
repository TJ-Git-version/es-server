package com.felix.esmysqlsync.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductListVO {

    @Schema(description = "总条数", example = "100")
    private Long total;

    @Schema(description = "商品列表", example = "[]")
    List<ProductEsVO> productEsVOList;

}
