package com.community.prime.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 商品订单DTO
 */
@Data
public class ProductOrderDTO {

    /**
     * 商品ID
     */
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /**
     * 购买数量
     */
    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量至少为1")
    private Integer quantity;

    /**
     * 收货地址
     */
    @NotBlank(message = "收货地址不能为空")
    private String address;

    /**
     * 收货人手机号
     */
    @NotBlank(message = "手机号不能为空")
    private String phone;
}
