package com.chengxun.gamemaker.service;

/**
 * 构建策略接口
 * 每种项目类型对应一个构建策略实现，负责项目的编译、启动、就绪检查和停止。
 *
 * <p>设计原则：</p>
 * <ul>
 *   <li>开放性：支持任意语言和框架</li>
 *   <li>自动检测：根据项目文件自动选择策略</li>
 *   <li>可扩展：通过注册新策略支持新的项目类型</li>
 * </ul>
 *
 * <p>实现类需标注 {@link org.springframework.stereotype.Component} 注解以自动注册。</p>
 *
 * @author chengxun
 * @since 1.0.0
 */
public interface BuildStrategy {

    /**
     * 获取策略优先级（数值越小优先级越高）
     *
     * @return 优先级数值
     */
    int getPriority();

    /**
     * 判断该策略是否能处理指定目录下的项目
     * 通常通过检测关键文件（如 package.json、pom.xml 等）来判断
     *
     * @param projectDir 项目根目录路径
     * @return 如果该策略能处理此项目返回 true，否则返回 false
     */
    boolean canHandle(String projectDir);

    /**
     * 执行项目构建
     *
     * @param projectDir 项目根目录路径
     * @return 构建结果，包含成功/失败状态、输出信息和耗时
     */
    BuildResult build(String projectDir);

    /**
     * 启动项目
     *
     * @param projectDir 项目根目录路径
     * @param port 启动端口号
     * @return 启动的进程对象
     * @throws Exception 启动失败时抛出异常
     */
    Process start(String projectDir, int port) throws Exception;

    /**
     * 检查项目是否已就绪（可以接受请求）
     * 实现时应设置合理的超时时间，避免永久阻塞
     *
     * @param projectDir 项目根目录路径
     * @param port 监听端口号
     * @return 如果项目已就绪返回 true，超时或失败返回 false
     */
    boolean isReady(String projectDir, int port);

    /**
     * 停止项目进程
     * 实现时应确保子进程也被正确终止，避免僵尸进程
     *
     * @param process 要停止的进程对象
     */
    void stop(Process process);

    /**
     * 获取项目类型名称
     *
     * @return 项目类型的可读名称，如 "npm"、"maven" 等
     */
    String getProjectType();

    /**
     * 获取默认启动端口
     *
     * @return 默认端口号，默认为 18100
     */
    default int getDefaultPort() {
        return 18100;
    }

    /**
     * 获取启动超时时间（秒）
     *
     * @return 超时秒数，默认为 30 秒
     */
    default int getStartTimeoutSeconds() {
        return 30;
    }
}
