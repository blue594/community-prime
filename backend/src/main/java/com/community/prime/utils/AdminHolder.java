package com.community.prime.utils;

import com.community.prime.entity.Admin;

/**
 * 管理员信息持有者（基于ThreadLocal）
 */
public class AdminHolder {

    private static final ThreadLocal<Admin> adminThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<String> tokenThreadLocal = new ThreadLocal<>();

    /**
     * 保存管理员信息
     *
     * @param admin 管理员信息
     */
    public static void saveAdmin(Admin admin) {
        adminThreadLocal.set(admin);
    }

    /**
     * 获取当前登录管理员
     *
     * @return 管理员信息
     */
    public static Admin getAdmin() {
        return adminThreadLocal.get();
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

    /**
     * 移除管理员信息
     */
    public static void removeAdmin() {
        adminThreadLocal.remove();
        tokenThreadLocal.remove();
    }
}
