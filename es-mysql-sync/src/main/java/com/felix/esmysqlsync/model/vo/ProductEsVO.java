package com.felix.esmysqlsync.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductEsVO {

    /**
     * 商品主键ID
     */
    private Long id;

    /**
     * 商品唯一编码（业务编号）
     */
    private String productNo;

    /**
     * 高亮：商品编码
     */
    private String highlightProductNo;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 高亮：商品名称
     */
    private String highlightProductName;

    /**
     * 分类ID
     */
    private Integer categoryId;

    /**
     * 品牌ID
     */
    private Integer brandId;

    /**
     * 销售价
     */
    private BigDecimal price;

    /**
     * 市场价
     */
    private BigDecimal marketPrice;

    /**
     * 成本价
     */
    private BigDecimal costPrice;

    /**
     * 库存
     */
    private Integer stock;

    /**
     * 状态 1=上架 2=下架 3=删除
     */
    private Integer status;

    /**
     * 排序权重
     */
    private Integer sort;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

}
