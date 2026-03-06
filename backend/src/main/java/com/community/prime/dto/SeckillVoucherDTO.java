package com.community.prime.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀券DTO
 */
@Data
public class SeckillVoucherDTO {

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
     * 库存
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
}
