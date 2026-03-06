package com.community.prime.vo;

import lombok.Data;

/**
 * 用户视图对象
 */
@Data
public class UserVO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 手机号（脱敏）
     */
    private String phone;

    /**
     * 昵称
     */
    private String nickName;

    /**
     * 头像
     */
    private String icon;

    /**
     * Token
     */
    private String token;
}
