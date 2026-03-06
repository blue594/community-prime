package com.community.prime;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 邻里优享 - 社区生活服务平台启动类
 * 
 * 核心业务：
 * 1. 超市网购（次日达）
 * 2. 餐饮到店优惠券
 * 3. 家政维修预约
 * 
 * 技术亮点：
 * - Redis分布式锁（Redisson）
 * - 缓存击穿/穿透/雪崩防护
 * - Redis Stream消息队列
 * - 秒杀高并发处理
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@MapperScan("com.community.prime.mapper")
public class CommunityPrimeApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommunityPrimeApplication.class, args);
        System.out.println("========================================");
        System.out.println("   邻里优享 (Community Prime) 启动成功!");
        System.out.println("   访问地址: http://localhost:8080/api");
        System.out.println("========================================");
    }
}
