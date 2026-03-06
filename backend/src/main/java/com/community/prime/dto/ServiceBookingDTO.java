package com.community.prime.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 服务预约DTO
 */
@Data
public class ServiceBookingDTO {

    /**
     * 服务类型ID
     */
    @NotNull(message = "服务类型不能为空")
    private Long serviceTypeId;

    /**
     * 预约日期
     */
    @NotNull(message = "预约日期不能为空")
    private LocalDate bookingDate;

    /**
     * 预约时间段开始
     */
    @NotNull(message = "预约时间不能为空")
    private LocalTime startTime;

    /**
     * 服务地址
     */
    @NotBlank(message = "服务地址不能为空")
    private String address;

    /**
     * 联系电话
     */
    @NotBlank(message = "联系电话不能为空")
    private String phone;

    /**
     * 备注
     */
    private String remark;
}
