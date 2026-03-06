package com.community.prime.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品秒杀实体类 - 记录参与秒杀的商品信息
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("tb_seckill_product")
public class SeckillProduct implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联商品ID
     */
    private Long productId;

    /**
     * 秒杀价格
     */
    private BigDecimal seckillPrice;

    /**
     * 秒杀库存
     */
    private Integer stock;

    /**
     * 秒杀开始时间
     */
    private LocalDateTime beginTime;

    /**
     * 秒杀结束时间
     */
    private LocalDateTime endTime;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;

    // ========== 以下为非数据库字段，用于页面展示 ==========

    /**
     * 商品名称（关联查询）
     */
    @TableField(exist = false)
    private String productName;

    /**
     * 商品图片（关联查询）
     */
    @TableField(exist = false)
    private String productImage;

    /**
     * 商品原价（关联查询）
     */
    @TableField(exist = false)
    private BigDecimal originalPrice;
}
