package com.felix.esmysqlsync.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 商品表
 */
@TableName(value ="product")
@Data
@Schema(description = "商品实体")
public class ProductEntity {
    @Schema(description = "商品主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "商品唯一编码（业务编号）")
    @TableField(value = "product_no")
    private String productNo;

    @Schema(description = "商品名称")
    @TableField(value = "product_name")
    private String productName;

    @Schema(description = "分类ID")
    @TableField(value = "category_id")
    private Integer categoryId;

    @Schema(description = "品牌ID")
    @TableField(value = "brand_id")
    private Integer brandId;

    @Schema(description = "销售价")
    @TableField(value = "price")
    private BigDecimal price;

    @Schema(description = "市场价")
    @TableField(value = "market_price")
    private BigDecimal marketPrice;

    @Schema(description = "成本价")
    @TableField(value = "cost_price")
    private BigDecimal costPrice;

    @Schema(description = "库存")
    @TableField(value = "stock")
    private Integer stock;

    @Schema(description = "状态：1=上架 2=下架 3=删除")
    @TableField(value = "status")
    private Integer status;

    @Schema(description = "排序权重")
    @TableField(value = "sort")
    private Integer sort;

    @Schema(description = "创建时间")
    @TableField(value = "create_time")
    private Date createTime;

    @Schema(description = "更新时间")
    @TableField(value = "update_time")
    private Date updateTime;
}