package com.community.prime.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券订单实体类 - 秒杀订单
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("tb_voucher_order")
public class VoucherOrder implements Serializable {

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
     * 优惠券ID
     */
    private Long voucherId;

    /**
     * 支付方式 1-余额支付 2-支付宝 3-微信
     */
    private Integer payType;

    /**
     * 订单状态 0-未支付 1-已支付 2-已核销 3-已取消 4-退款中 5-已退款
     */
    private Integer status;

    /**
     * 下单时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 支付时间
     */
    private LocalDateTime payTime;

    /**
     * 核销时间
     */
    private LocalDateTime useTime;

    /**
     * 退款时间
     */
    private LocalDateTime refundTime;

    /**
     * 核销码（支付后生成，用于到店核销）
     */
    private String verifyCode;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
