package com.community.prime.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品视图对象
 */
@Data
public class ProductVO {

    /**
     * 商品ID
     */
    private Long id;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 商品图片
     */
    private String image;

    /**
     * 价格
     */
    private BigDecimal price;

    /**
     * 库存
     */
    private Integer stock;

    /**
     * 销量
     */
    private Integer sold;

    /**
     * 商品描述
     */
    private String description;

    /**
     * 状态 0-下架 1-上架
     */
    private Integer status;
}
