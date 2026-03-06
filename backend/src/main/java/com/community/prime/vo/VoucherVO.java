package com.community.prime.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券视图对象
 */
@Data
public class VoucherVO {

    /**
     * 券ID
     */
    private Long id;

    /**
     * 商家ID
     */
    private Long shopId;

    /**
     * 优惠券标题
     */
    private String title;

    /**
     * 副标题
     */
    private String subTitle;

    /**
     * 使用规则
     */
    private String rules;

    /**
     * 支付金额（秒杀价）
     */
    private BigDecimal payValue;

    /**
     * 抵扣金额（原价）
     */
    private BigDecimal actualValue;

    /**
     * 库存（秒杀券用）
     */
    private Integer stock;

    /**
     * 生效时间
     */
    private LocalDateTime beginTime;

    /**
     * 失效时间
     */
    private LocalDateTime endTime;

    /**
     * 优惠券类型 0-普通优惠券 1-秒杀优惠券
     */
    private Integer type;

    /**
     * 状态 0-未开始 1-进行中 2-已结束
     */
    private Integer status;
}
