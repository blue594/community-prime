package com.community.prime.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 服务预约实体类 - 家政/维修预约
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("tb_service_booking")
public class ServiceBooking implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 服务类型ID
     */
    private Long serviceTypeId;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 服务价格
     */
    private BigDecimal price;

    /**
     * 预约日期
     */
    private LocalDate bookingDate;

    /**
     * 预约时间段开始
     */
    private LocalTime startTime;

    /**
     * 预约时间段结束
     */
    private LocalTime endTime;

    /**
     * 服务地址
     */
    private String address;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 备注
     */
    private String remark;

    /**
     * 预约状态 0-待确认 1-已确认 2-服务中 3-已完成 4-已取消
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

    /**
     * 是否已评价（非数据库字段，查询时动态填充）
     */
    @TableField(exist = false)
    private Boolean reviewed;
}
