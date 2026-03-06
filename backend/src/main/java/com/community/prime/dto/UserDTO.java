package com.community.prime.dto;

import lombok.Data;

/**
 * 用户信息DTO（脱敏后返回给前端）
 */
@Data
public class UserDTO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 昵称
     */
    private String nickName;

    /**
     * 头像
     */
    private String icon;
}
