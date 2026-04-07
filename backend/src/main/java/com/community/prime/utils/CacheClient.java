package com.community.prime.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.community.prime.utils.RedisConstants.*;

// todo
/**
 * 【面试重点】Redis缓存工具类 - 缓存三剑客解决方案
 * 
 * 【面试点 1】缓存穿透解决方案
 * - 问题：大量请求查询不存在的数据，直接打到数据库
 * - 解决：缓存空值（设置较短过期时间），如 queryWithPassThrough 方法
 * 
 * 【面试点 2】缓存击穿解决方案
 * - 问题：热点key突然过期，大量请求同时打到数据库
 * - 解决：
 *   a) 互斥锁：queryWithMutex 方法，使用 setnx 实现分布式锁
 *   b) 逻辑过期：queryWithLogicalExpire 方法，热点数据永不过期
 * 
 * 【面试点 3】缓存雪崩解决方案
 * - 问题：大量key同时过期，数据库压力剧增
 * - 解决：随机过期时间，基础时间 + 0-10分钟随机值
 * 
 * 【面试点 4】互斥锁 vs 逻辑过期对比
 * - 互斥锁：保证数据一致性，但性能有损耗（串行执行）
 * - 逻辑过期：性能优异，但数据可能短暂不一致
 * - 选择：一致性要求高用互斥锁，性能要求高用逻辑过期
 */
@Slf4j
@Component
public class CacheClient {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    /**
     * 将对象序列化为JSON并存储，设置TTL（随机过期时间防雪崩）
     * 
     * @param key   Redis key
     * @param value 值
     * @param time  基础过期时间
     * @param unit  时间单位
     */
    public void set(String key, Object value, Long time, TimeUnit unit) {
        // 随机过期时间，防止缓存雪崩：基础时间 + 0-10分钟的随机值
        long randomTime = time + (long) (Math.random() * 600);
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), randomTime, unit);
    }

    /**
     * 设置逻辑过期时间
     * 
     * @param key   Redis key
     * @param value 值
     * @param time  过期时间
     * @param unit  时间单位
     */
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        // 设置逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 根据key查询缓存，反序列化为指定类型
     * 
     * @param keyPrefix  key前缀
     * @param id         ID
     * @param type       返回类型
     * @param dbFallback 数据库查询函数
     * @param time       缓存时间
     * @param unit       时间单位
     * @param <R>        返回类型
     * @param <ID>       ID类型
     * @return 查询结果
     */
    /**
     * 【面试点】缓存穿透解决方案 - 缓存空值
     * 
     * 实现要点：
     * 1. 查询缓存为空时，再查数据库
     * 2. 数据库也不存在，将空值""写入Redis
     * 3. 空值设置较短过期时间（如2分钟），避免长期占用内存
     */
    public <R, ID> R queryWithPassThrough(
            String keyPrefix, ID id, Class<R> type,
            Function<ID, R> dbFallback, Long time, TimeUnit unit) {

        String key = keyPrefix + id;
        // 1. 从Redis查询缓存
        String json = stringRedisTemplate.opsForValue().get(key);

        // 2. 判断是否存在
        if (StrUtil.isNotBlank(json)) {
            // 3. 存在，直接返回
            return JSONUtil.toBean(json, type);
        }

        // 【面试点】判断命中的是否是空值（防止缓存穿透）
        // json 为 "" 空字符串时，说明之前查询过且数据库不存在
        if (json != null) {
            return null;
        }

        // 4. 不存在，根据id查询数据库
        @SuppressWarnings("unchecked")
        R r = (R) dbFallback.apply(id);

        // 5. 不存在，返回错误
        if (r == null) {
            // 【面试点】将空值写入Redis，设置较短过期时间，防止缓存穿透
            stringRedisTemplate.opsForValue().set(key, "", PRODUCT_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }

        // 6. 存在，写入Redis（随机过期时间防雪崩）
        this.set(key, r, time, unit);

        return r;
    }

    /**
     * 【面试点】缓存击穿解决方案 - 互斥锁
     * 
     * 实现要点：
     * 1. 使用 Redis setnx 实现分布式互斥锁
     * 2. 获取锁失败则休眠重试（递归调用）
     * 3. 获取锁成功后 Double Check，避免重复查询数据库
     * 4. 使用 try-finally 确保锁最终释放
     * 
     * 优缺点：
     * - 优点：保证数据强一致性
     * - 缺点：性能有损耗，同一时刻只有一个线程能访问数据库
     */
    public <R, ID> R queryWithMutex(
            String keyPrefix, ID id, Class<R> type,
            Function<ID, R> dbFallback, Long time, TimeUnit unit) {

        String key = keyPrefix + id;
        // 1. 从Redis查询缓存
        String json = stringRedisTemplate.opsForValue().get(key);

        // 2. 判断是否存在
        if (StrUtil.isNotBlank(json)) {
            // 3. 存在，直接返回
            return JSONUtil.toBean(json, type);
        }

        // 判断命中的是否是空值
        if (json != null) {
            return null;
        }

        // 4. 实现缓存重建
        // 4.1 获取互斥锁
        String lockKey = LOCK_KEY_PREFIX + keyPrefix + id;
        R r = null;
        try {
            boolean isLock = tryLock(lockKey);
            // 4.2 判断是否获取成功
            if (!isLock) {
                // 4.3 失败，则休眠重试
                Thread.sleep(LOCK_RETRY_INTERVAL);
                return queryWithMutex(keyPrefix, id, type, dbFallback, time, unit);
            }

            // 【面试点】Double Check：获取锁后再次检查缓存
            // 防止在等待锁的过程中，其他线程已经重建了缓存
            json = stringRedisTemplate.opsForValue().get(key);
            if (StrUtil.isNotBlank(json)) {
                return JSONUtil.toBean(json, type);
            }

            // 4.5 根据id查询数据库
            r = dbFallback.apply(id);

            // 5. 不存在，返回错误
            if (r == null) {
                stringRedisTemplate.opsForValue().set(key, "", PRODUCT_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }

            // 6. 存在，写入Redis
            this.set(key, r, time, unit);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            // 7. 释放锁（必须放在finally中确保释放）
            unlock(lockKey);
        }

        return r;
    }

    /**
     * 【面试点】缓存击穿解决方案 - 逻辑过期
     * 
     * 核心思想：热点数据永不过期，而是设置一个逻辑过期时间字段
     * 
     * 实现要点：
     * 1. 缓存数据中添加 expireTime 逻辑过期时间字段
     * 2. 查询时发现逻辑时间已过期，则获取锁开启异步线程重建缓存
     * 3. 无论是否获取锁成功，都立即返回过期数据（保证可用性）
     * 
     * 优缺点：
     * - 优点：性能优异，无需等待缓存重建
     * - 缺点：数据可能短暂不一致（返回的是过期数据）
     * 
     * 适用场景：对一致性要求不高的热点数据（如商品详情页）
     */
    public <R, ID> R queryWithLogicalExpire(
            String keyPrefix, ID id, Class<R> type,
            Function<ID, R> dbFallback, Long time, TimeUnit unit) {

        String key = keyPrefix + id;
        // 1. 从Redis查询缓存
        String json = stringRedisTemplate.opsForValue().get(key);

        // 2. 判断是否存在
        if (StrUtil.isBlank(json)) {
            // 3. 不存在，直接返回null（热点数据需要提前预热）
            return null;
        }

        // 4. 命中，需要先把json反序列化为对象
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        LocalDateTime expireTime = redisData.getExpireTime();

        // 5. 判断是否过期
        if (expireTime.isAfter(LocalDateTime.now())) {
            // 5.1 未过期，直接返回
            return r;
        }

        // 5.2 已过期，需要缓存重建
        // 6. 缓存重建
        // 6.1 获取互斥锁
        String lockKey = LOCK_KEY_PREFIX + keyPrefix + id;
        boolean isLock = tryLock(lockKey);

        // 6.2 判断是否获取锁成功
        if (isLock) {
            // 6.3 成功，开启独立线程，实现缓存重建（异步）
            // 【面试点】使用线程池异步重建，不阻塞主线程响应
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    // 查询数据库
                    R r1 = (R) dbFallback.apply(id);
                    // 写入Redis
                    this.setWithLogicalExpire(key, r1, time, unit);
                } catch (Exception e) {
                    log.error("缓存重建失败", e);
                } finally {
                    unlock(lockKey);
                }
            });
        }
        // todo
        // 【面试点】6.4 无论是否获取锁，都立即返回过期数据（保证高可用）
        return r;
    }

    /**
     * 尝试获取锁
     * 
     * @param key 锁key
     * @return 是否获取成功
     */
    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", LOCK_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    /**
     * 释放锁
     * 
     * @param key 锁key
     */
    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }

    /**
     * 删除缓存
     * 
     * @param key 缓存key
     */
    public void delete(String key) {
        stringRedisTemplate.delete(key);
    }

    /**
     * 带逻辑过期时间的Redis数据封装
     */
    @lombok.Data
    static class RedisData {
        private LocalDateTime expireTime;
        private Object data;
    }
}
