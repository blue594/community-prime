package com.community.prime.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson配置类
 * 
 * 技术说明：
 * 1. Redisson是Redis的Java客户端，提供了分布式锁等高级功能
 * 2. 支持可重入锁、公平锁、红锁等多种锁类型
 * 3. 支持锁的自动续期（Watch Dog机制）
 * 4. 用于实现秒杀场景的分布式锁
 */
@Configuration
public class RedissonConfig {

    @Value("${redisson.address}")
    private String address;

    @Value("${redisson.password:}")
    private String password;

    @Value("${redisson.database:0}")
    private Integer database;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress(address)
                .setDatabase(database);
        
        if (password != null && !password.isEmpty()) {
            config.useSingleServer().setPassword(password);
        }

        return Redisson.create(config);
    }
}
