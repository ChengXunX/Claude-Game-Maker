package com.chengxun.gamemaker.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步处理配置
 * 配置异步任务执行器，用于处理耗时操作
 *
 * 主要功能：
 * - 配置线程池参数
 * - 支持异步方法执行
 * - 提供多个异步执行器
 *
 * @author chengxun
 * @since 1.0.0
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    /**
     * 默认异步执行器
     * 用于一般的异步任务
     *
     * @return 线程池执行器
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-task-");
        executor.setRejectedExecutionHandler((r, e) -> {
            log.warn("异步任务被拒绝，队列已满");
            // 可以选择抛出异常或记录日志
        });
        executor.initialize();
        log.info("异步任务执行器初始化完成，核心线程数: 5, 最大线程数: 20");
        return executor;
    }

    /**
     * Agent 任务执行器
     * 用于 Agent 相关的异步任务
     *
     * @return 线程池执行器
     */
    @Bean(name = "agentTaskExecutor")
    public Executor agentTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("agent-task-");
        executor.initialize();
        log.info("Agent 任务执行器初始化完成，核心线程数: 10, 最大线程数: 50");
        return executor;
    }

    /**
     * 通知任务执行器
     * 用于发送通知的异步任务
     *
     * @return 线程池执行器
     */
    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("notification-");
        executor.initialize();
        log.info("通知任务执行器初始化完成，核心线程数: 3, 最大线程数: 10");
        return executor;
    }
}
