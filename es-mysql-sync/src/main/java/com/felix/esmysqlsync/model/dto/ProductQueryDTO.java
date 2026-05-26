package com.felix.esmysqlsync.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "商品查询条件")
public class ProductQueryDTO {

    @Schema(description = "商品名称", example = "小米手机")
    private String productName;

    @Schema(description = "商品编码", example = "P001")
    private String productNo;

    @Schema(description = "分类ID", example = "1")
    private Long categoryId;

    @Schema(description = "品牌ID", example = "1")
    private Long brandId;

    @Schema(description = "状态：1=上架 2=下架 3=删除", example = "1")
    private Integer status;

    @Schema(description = "最小销售价", example = "1000.00")
    private BigDecimal minPrice;

    @Schema(description = "最大销售价", example = "9999.00")
    private BigDecimal maxPrice;

    @Schema(description = "最小市场价", example = "1000.00")
    private BigDecimal minMarketPrice;

    @Schema(description = "最大市场价", example = "9999.00")
    private BigDecimal maxMarketPrice;

    @Schema(description = "最小成本价", example = "500.00")
    private BigDecimal minCostPrice;

    @Schema(description = "最大成本价", example = "5000.00")
    private BigDecimal maxCostPrice;

    @Schema(description = "是否有库存", example = "true")
    private Boolean hasStock;

    @Schema(description = "最小库存", example = "0")
    private Integer minStock;

    @Schema(description = "最大库存", example = "1000")
    private Integer maxStock;

    @Schema(description = "创建开始时间", example = "2024-01-01 00:00:00")
    private Date createTimeStart;

    @Schema(description = "创建结束时间", example = "2024-12-31 23:59:59")
    private Date createTimeEnd;

    @Schema(description = "更新开始时间", example = "2024-01-01 00:00:00")
    private Date updateTimeStart;

    @Schema(description = "更新结束时间", example = "2024-12-31 23:59:59")
    private Date updateTimeEnd;

    @Schema(description = "页码", example = "1")
    private Integer pageNum;

    @Schema(description = "每页数量", example = "10")
    private Integer pageSize;
}
