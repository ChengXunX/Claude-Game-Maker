package com.chengxun.gamemaker.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 分布式锁配置类
 * 基于Redisson实现分布式锁
 *
 * 主要功能：
 * - 配置Redisson客户端
 * - 提供分布式锁支持
 * - 支持多种锁类型（可重入锁、公平锁、读写锁等）
 *
 * 使用场景：
 * - Agent创建时防止重复创建
 * - 任务分配时防止重复分配
 * - 配置更新时防止并发冲突
 *
 * @author chengxun
 * @since 1.0.0
 */
@Configuration
public class DistributedLockConfig {

    private static final Logger log = LoggerFactory.getLogger(DistributedLockConfig.class);

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.database:0}")
    private int redisDatabase;

    /**
     * 配置Redisson客户端
     * 支持单机、集群、哨兵模式
     *
     * @return RedissonClient实例
     */
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();

        // 构建Redis连接地址
        String redisAddress = String.format("redis://%s:%d", redisHost, redisPort);
        config.useSingleServer().setAddress(redisAddress);

        // 设置密码（如果有）
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.useSingleServer().setPassword(redisPassword);
        }

        // 设置数据库
        config.useSingleServer().setDatabase(redisDatabase);

        // 连接池配置
        config.useSingleServer().setConnectionMinimumIdleSize(5);
        config.useSingleServer().setConnectionPoolSize(20);
        config.useSingleServer().setIdleConnectionTimeout(10000);
        config.useSingleServer().setConnectTimeout(30000);
        config.useSingleServer().setTimeout(30000);

        log.info("Redisson客户端配置完成 - 地址: {}", redisAddress);
        return Redisson.create(config);
    }
}
