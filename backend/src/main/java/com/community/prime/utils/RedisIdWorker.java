package com.community.prime.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

// todo
/**
 * 【面试重点】Redis分布式ID生成器 - 全局唯一ID解决方案
 * 
 * 【面试点 1】分布式ID生成方案对比
 * - UUID：无序，占用空间大，不适合做主键
 * - 数据库自增：单机可用，分布式环境下存在瓶颈
 * - Redis自增：性能高，单机可达数万QPS，但依赖Redis
 * - 雪花算法：时间戳+机器ID+序列号，不依赖第三方，但依赖系统时钟
 * 
 * 【面试点 2】本项目ID结构（64位Long类型）
 * - 符号位：1bit（始终为0，保证正数）
 * - 时间戳：31bit（支持约68年）
 * - 序列号：32bit（每天可生成约42亿个ID）
 * 
 * 【面试点 3】优势
 * - 趋势递增：时间戳在高位，ID整体趋势递增，利于数据库索引
 * - 高性能：基于Redis自增，单机性能优异
 * - 高可用：每天一个key，避免单key过大，同时便于统计日活
 */
@Component
public class RedisIdWorker {

    /**
     * 开始时间戳（2024-01-01 00:00:00）
     */
    private static final long BEGIN_TIMESTAMP = 1704067200L;

    /**
     * 序列号位数
     */
    private static final int COUNT_BITS = 32;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 【面试点】生成唯一ID
     * 
     * 实现步骤：
     * 1. 计算时间戳差值 = 当前时间 - 起始时间（2024-01-01）
     * 2. 使用Redis自增生成序列号（每天一个key）
     * 3. 位运算拼接：时间戳左移32位 + 序列号
     * 
     * key设计：icr:业务前缀:yyyy:MM:dd
     * - icr：icr（incr缩写）表示自增
     * - 业务前缀：区分不同业务（如order、voucher）
     * - 日期：每天一个key，避免单key过大
     */
    public long nextId(String keyPrefix) {
        // 1. 生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;

        // 2. 生成序列号
        // 2.1 获取当前日期，精确到天
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        // 【面试点】2.2 Redis自增，每天一个key
        // 优势：避免单key数据过大，同时可以统计每天的订单量/发券量
        long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);

        // 【面试点】3. 位运算拼接ID
        // timestamp << COUNT_BITS：时间戳左移32位，腾出低32位给序列号
        // | count：按位或运算，将序列号拼接到低32位
        return timestamp << COUNT_BITS | count;
    }
}
