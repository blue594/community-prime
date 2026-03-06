package com.community.prime.utils;

/**
 * Redis常量
 */
public class RedisConstants {

    /**
     * 登录验证码key前缀
     */
    public static final String LOGIN_CODE_KEY = "login:code:";

    /**
     * 登录验证码有效期（分钟）
     */
    public static final Long LOGIN_CODE_TTL = 2L;

    /**
     * 登录用户key前缀
     */
    public static final String LOGIN_USER_KEY = "login:token:";

    /**
     * 登录用户有效期（分钟）
     */
    public static final Long LOGIN_USER_TTL = 30L;

    /**
     * 商品缓存key前缀
     */
    public static final String PRODUCT_CACHE_KEY = "product:cache:";

    /**
     * 商品缓存有效期（分钟）- 随机范围基础值
     */
    public static final Long PRODUCT_CACHE_TTL = 30L;

    /**
     * 商品缓存空值有效期（分钟）- 防止缓存穿透
     */
    public static final Long PRODUCT_NULL_TTL = 2L;

    /**
     * 优惠券缓存key前缀
     */
    public static final String VOUCHER_CACHE_KEY = "voucher:cache:";

    /**
     * 秒杀库存key前缀
     */
    public static final String SECKILL_STOCK_KEY = "seckill:stock:";

    /**
     * 秒杀订单key前缀
     */
    public static final String SECKILL_ORDER_KEY = "seckill:order:";

    /**
     * 分布式锁key前缀
     */
    public static final String LOCK_KEY_PREFIX = "lock:";

    /**
     * 分布式锁key - 商品缓存重建
     */
    public static final String LOCK_PRODUCT_KEY = "lock:product:";

    /**
     * 分布式锁key - 秒杀下单
     */
    public static final String LOCK_SECKILL_KEY = "lock:seckill:";

    /**
     * 分布式锁有效期（秒）
     */
    public static final Long LOCK_TTL = 10L;

    /**
     * 互斥锁等待重试间隔（毫秒）
     */
    public static final Long LOCK_RETRY_INTERVAL = 50L;

    /**
     * 互斥锁最大重试次数
     */
    public static final Integer LOCK_MAX_RETRIES = 200;

    /**
     * 管理员登录key前缀
     */
    public static final String LOGIN_ADMIN_KEY = "login:admin:";

    /**
     * 管理员登录有效期（分钟）
     */
    public static final Long LOGIN_ADMIN_TTL = 30L;
}
