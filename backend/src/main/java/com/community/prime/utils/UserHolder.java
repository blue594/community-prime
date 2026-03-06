package com.community.prime.utils;

import com.community.prime.dto.UserDTO;

/**
 * 用户上下文持有器
 *
 * 使用ThreadLocal存储当前登录用户信息
 * 保证线程安全，避免参数传递
 */
public class UserHolder {

    private static final ThreadLocal<UserDTO> tl = new ThreadLocal<>();
    private static final ThreadLocal<String> tokenThreadLocal = new ThreadLocal<>();

    public static void saveUser(UserDTO user) {
        tl.set(user);
    }

    public static UserDTO getUser() {
        return tl.get();
    }

    public static Long getUserId() {
        UserDTO user = tl.get();
        return user == null ? null : user.getId();
    }

    /**
     * 保存token
     *
     * @param token token
     */
    public static void saveToken(String token) {
        tokenThreadLocal.set(token);
    }

    /**
     * 获取token
     *
     * @return token
     */
    public static String getToken() {
        return tokenThreadLocal.get();
    }

    public static void removeUser() {
        tl.remove();
        tokenThreadLocal.remove();
    }
}
