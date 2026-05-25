package com.felix.esmysqlsync.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 商品表
 * @TableName product
 */
@TableName(value ="product")
@Data
public class ProductEntity {
    /**
     * 商品主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 商品唯一编码（业务编号）
     */
    @TableField(value = "product_no")
    private String productNo;

    /**
     * 商品名称
     */
    @TableField(value = "product_name")
    private String productName;

    /**
     * 分类ID
     */
    @TableField(value = "category_id")
    private Integer categoryId;

    /**
     * 品牌ID
     */
    @TableField(value = "brand_id")
    private Integer brandId;

    /**
     * 销售价
     */
    @TableField(value = "price")
    private BigDecimal price;

    /**
     * 市场价
     */
    @TableField(value = "market_price")
    private BigDecimal marketPrice;

    /**
     * 成本价
     */
    @TableField(value = "cost_price")
    private BigDecimal costPrice;

    /**
     * 库存
     */
    @TableField(value = "stock")
    private Integer stock;

    /**
     * 状态 1=上架 2=下架 3=删除
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 排序权重
     */
    @TableField(value = "sort")
    private Integer sort;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time")
    private Date updateTime;
}