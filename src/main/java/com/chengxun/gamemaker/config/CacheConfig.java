package com.chengxun.gamemaker.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 缓存配置
 * 配置Spring Cache缓存管理器
 *
 * 主要功能：
 * - 启用Spring Cache注解
 * - 配置Redis缓存管理器（生产环境）
 * - 配置内存缓存管理器（开发环境）
 * - 定义缓存名称和过期时间
 *
 * @author chengxun
 * @since 1.0.0
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 缓存名称常量
     */
    public static final String CACHE_PROJECTS = "projects";
    public static final String CACHE_SKILLS = "skills";
    public static final String CACHE_AGENTS = "agents";
    public static final String CACHE_USERS = "users";
    public static final String CACHE_CONFIGS = "configs";
    public static final String CACHE_METRICS = "metrics";
    public static final String CACHE_ROLES = "roles";

    /**
     * 配置Redis缓存管理器（生产环境）
     * 当Redis连接可用时使用此配置
     *
     * @param connectionFactory Redis连接工厂
     * @return Redis缓存管理器
     */
    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 配置ObjectMapper支持Java 8日期时间类型，并启用类型信息以支持正确反序列化
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 启用Default Typing，确保Redis缓存反序列化时能正确还原对象类型
        objectMapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );

        // 创建支持Java 8日期时间的序列化器
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        // 默认缓存配置 - 允许缓存null值
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));

        // 为不同缓存设置不同的过期时间
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put(CACHE_USERS, defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigs.put(CACHE_PROJECTS, defaultConfig.entryTtl(Duration.ofHours(2)));
        cacheConfigs.put(CACHE_SKILLS, defaultConfig.entryTtl(Duration.ofHours(2)));
        cacheConfigs.put(CACHE_AGENTS, defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put(CACHE_CONFIGS, defaultConfig.entryTtl(Duration.ofHours(24)));
        cacheConfigs.put(CACHE_METRICS, defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigs.put(CACHE_ROLES, defaultConfig.entryTtl(Duration.ofHours(1)));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigs)
            .transactionAware()
            .build();
    }

    /**
     * 配置内存缓存管理器（开发环境备用）
     * 当Redis不可用时可以使用此配置
     *
     * @return 内存缓存管理器
     */
    @Bean(name = "simpleCacheManager")
    public CacheManager simpleCacheManager() {
        return new ConcurrentMapCacheManager(
            CACHE_PROJECTS,
            CACHE_SKILLS,
            CACHE_AGENTS,
            CACHE_USERS,
            CACHE_CONFIGS,
            CACHE_METRICS,
            CACHE_ROLES
        );
    }
}
