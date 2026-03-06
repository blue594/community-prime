package com.community.prime.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 服务类型实体类 - 家政/维修服务类型
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("tb_service_type")
public class ServiceType implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 服务名称
     */
    private String name;

    /**
     * 服务分类 1-家政 2-维修
     */
    private Integer category;

    /**
     * 服务图片
     */
    private String image;

    /**
     * 服务描述
     */
    private String description;

    /**
     * 基础价格
     */
    private BigDecimal basePrice;

    /**
     * 单位（小时/次/平米等）
     */
    private String unit;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 状态 0-下架 1-上架
     */
    private Integer status;

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
}
