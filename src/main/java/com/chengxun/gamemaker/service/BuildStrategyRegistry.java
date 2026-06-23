package com.chengxun.gamemaker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 构建策略注册中心
 * 管理所有构建策略实现，提供自动检测、构建、启动和停止游戏项目的统一入口。
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li>自动发现并注册所有 {@link BuildStrategy} 实现</li>
 *   <li>按优先级排序策略，优先级数值越小越优先</li>
 *   <li>根据项目目录自动匹配合适的构建策略</li>
 *   <li>封装游戏进程的启动和生命周期管理</li>
 * </ul>
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
public class BuildStrategyRegistry {

    private static final Logger log = LoggerFactory.getLogger(BuildStrategyRegistry.class);

    /** 所有已注册的构建策略，按优先级排序（数值越小越靠前） */
    private final List<BuildStrategy> strategies;

    /**
     * 构造函数，通过 Spring 自动注入所有 BuildStrategy 实现并按优先级排序
     *
     * @param strategies 自动注入的所有 BuildStrategy 实现列表
     */
    @Autowired
    public BuildStrategyRegistry(List<BuildStrategy> strategies) {
        // 按优先级数值升序排列，数值越小优先级越高
        this.strategies = strategies.stream()
                .sorted(Comparator.comparingInt(BuildStrategy::getPriority))
                .toList();

        log.info("已注册 {} 个构建策略，优先级顺序：", this.strategies.size());
        for (BuildStrategy strategy : this.strategies) {
            log.info("  [{}] {} - 优先级: {}", strategy.getProjectType(), strategy.getClass().getSimpleName(), strategy.getPriority());
        }
    }

    /**
     * 查找能处理指定项目目录的构建策略
     * 按优先级从高到低依次检测，返回第一个匹配的策略
     *
     * @param projectDir 项目根目录路径
     * @return 匹配的构建策略（Optional 包装），无匹配时返回 Optional.empty()
     */
    public Optional<BuildStrategy> findStrategy(String projectDir) {
        if (projectDir == null || projectDir.isBlank()) {
            log.warn("项目目录为空，无法查找构建策略");
            return Optional.empty();
        }

        for (BuildStrategy strategy : strategies) {
            try {
                if (strategy.canHandle(projectDir)) {
                    log.info("为项目目录 [{}] 匹配到构建策略: {} ({})", projectDir, strategy.getProjectType(), strategy.getClass().getSimpleName());
                    return Optional.of(strategy);
                }
            } catch (Exception e) {
                log.warn("策略 {} 检查目录 {} 时发生异常: {}", strategy.getProjectType(), projectDir, e.getMessage());
            }
        }

        log.warn("未找到能处理项目目录 [{}] 的构建策略", projectDir);
        return Optional.empty();
    }

    /**
     * 执行项目构建
     * 自动检测项目类型并执行对应的构建流程
     *
     * @param projectDir 项目根目录路径
     * @return 构建结果
     * @throws IllegalStateException 如果没有找到匹配的构建策略
     */
    public BuildResult build(String projectDir) {
        BuildStrategy strategy = findStrategy(projectDir)
                .orElseThrow(() -> new IllegalStateException("未找到能处理目录 [" + projectDir + "] 的构建策略"));

        log.info("开始构建项目 [{}]，使用策略: {}", projectDir, strategy.getProjectType());
        long startTime = System.currentTimeMillis();

        BuildResult result = strategy.build(projectDir);

        result.setBuildType(strategy.getProjectType());
        log.info("项目 [{}] 构建{}，耗时: {}ms", projectDir, result.isSuccess() ? "成功" : "失败", result.getDurationMs());
        return result;
    }

    /**
     * 启动游戏项目
     * 自动检测项目类型，使用默认端口启动并封装为 GameProcess 对象
     *
     * @param projectDir 项目根目录路径
     * @return 游戏进程封装对象
     * @throws IllegalStateException 如果没有找到匹配的构建策略
     * @throws Exception 启动失败时抛出
     */
    public GameProcess startGame(String projectDir) throws Exception {
        BuildStrategy strategy = findStrategy(projectDir)
                .orElseThrow(() -> new IllegalStateException("未找到能处理目录 [" + projectDir + "] 的构建策略"));

        int port = strategy.getDefaultPort();
        return startGame(projectDir, port);
    }

    /**
     * 启动游戏项目（指定端口）
     * 自动检测项目类型并在指定端口启动，封装为 GameProcess 对象
     *
     * @param projectDir 项目根目录路径
     * @param port 启动端口号
     * @return 游戏进程封装对象
     * @throws IllegalStateException 如果没有找到匹配的构建策略
     * @throws Exception 启动失败时抛出
     */
    public GameProcess startGame(String projectDir, int port) throws Exception {
        BuildStrategy strategy = findStrategy(projectDir)
                .orElseThrow(() -> new IllegalStateException("未找到能处理目录 [" + projectDir + "] 的构建策略"));

        log.info("启动游戏项目 [{}]，策略: {}，端口: {}", projectDir, strategy.getProjectType(), port);

        // 如果端口为 0 或 -1，使用默认端口
        if (port <= 0) {
            port = strategy.getDefaultPort();
        }

        Process process = strategy.start(projectDir, port);
        return new GameProcess(strategy, process, port, projectDir);
    }

    /**
     * 获取所有已注册的构建策略
     *
     * @return 策略列表（已按优先级排序）
     */
    public List<BuildStrategy> getStrategies() {
        return strategies;
    }

    /**
     * 游戏进程内部类
     * 封装了构建策略、进程对象和端口信息，提供便捷的就绪检查和停止操作。
     *
     * <p>使用 {@link BuildStrategyRegistry#startGame} 方法创建实例。</p>
     */
    public static class GameProcess {

        /** 使用的构建策略 */
        private final BuildStrategy strategy;

        /** 底层进程对象 */
        private final Process process;

        /** 监听端口 */
        private final int port;

        /** 项目目录路径 */
        private final String projectDir;

        /** 进程启动时间 */
        private final long startTimeMs;

        /**
         * 构造函数
         *
         * @param strategy 使用的构建策略
         * @param process 底层进程对象
         * @param port 监听端口
         * @param projectDir 项目目录路径
         */
        public GameProcess(BuildStrategy strategy, Process process, int port, String projectDir) {
            this.strategy = strategy;
            this.process = process;
            this.port = port;
            this.projectDir = projectDir;
            this.startTimeMs = System.currentTimeMillis();
        }

        /**
         * 检查游戏进程是否已就绪（可以接受请求）
         * 会根据策略的超时配置等待，避免永久阻塞
         *
         * @return 如果已就绪返回 true，超时或失败返回 false
         */
        public boolean isReady() {
            try {
                return strategy.isReady(projectDir, port);
            } catch (Exception e) {
                return false;
            }
        }

        /**
         * 停止游戏进程
         * 会正确终止进程及其子进程
         */
        public void stop() {
            try {
                strategy.stop(process);
            } catch (Exception e) {
                // 停止失败时强制销毁进程
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
            }
        }

        /**
         * 检查进程是否仍在运行
         *
         * @return 进程存活返回 true
         */
        public boolean isAlive() {
            return process.isAlive();
        }

        /**
         * 获取进程运行时长（毫秒）
         *
         * @return 从启动到现在经过的毫秒数
         */
        public long getUptimeMs() {
            return System.currentTimeMillis() - startTimeMs;
        }

        /**
         * 获取使用的构建策略
         *
         * @return BuildStrategy 实例
         */
        public BuildStrategy getStrategy() {
            return strategy;
        }

        /**
         * 获取底层进程对象
         *
         * @return Process 实例
         */
        public Process getProcess() {
            return process;
        }

        /**
         * 获取监听端口
         *
         * @return 端口号
         */
        public int getPort() {
            return port;
        }

        /**
         * 获取项目目录路径
         *
         * @return 项目目录路径
         */
        public String getProjectDir() {
            return projectDir;
        }

        @Override
        public String toString() {
            return "GameProcess{" +
                    "type=" + strategy.getProjectType() +
                    ", port=" + port +
                    ", alive=" + process.isAlive() +
                    ", uptimeMs=" + getUptimeMs() +
                    '}';
        }
    }
}
