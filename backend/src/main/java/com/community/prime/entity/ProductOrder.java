package com.community.prime.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品订单实体类 - 超市商品订单
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("tb_product_order")
public class ProductOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键，订单ID
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 购买数量
     */
    private Integer quantity;

    /**
     * 订单总金额
     */
    private BigDecimal totalAmount;

    /**
     * 实付金额
     */
    private BigDecimal payAmount;

    /**
     * 收货地址
     */
    private String address;

    /**
     * 收货人手机号
     */
    private String phone;

    /**
     * 订单状态 0-待付款 1-待发货 2-待收货 3-已完成 4-已取消
     */
    private Integer status;

    /**
     * 支付方式
     */
    private Integer payType;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 支付时间
     */
    private LocalDateTime payTime;

    /**
     * 发货时间
     */
    private LocalDateTime deliveryTime;

    /**
     * 完成时间
     */
    private LocalDateTime finishTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
