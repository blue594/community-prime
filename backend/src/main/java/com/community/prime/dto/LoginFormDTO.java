package com.community.prime.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 登录表单DTO
 */
@Data
public class LoginFormDTO {

    /**
     * 手机号
     */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 验证码（验证码登录时使用）
     */
    private String code;

    /**
     * 密码（密码登录时使用）
     */
    private String password;

    /**
     * 登录类型：1-验证码登录 2-密码登录
     */
    private Integer loginType = 1;
}
