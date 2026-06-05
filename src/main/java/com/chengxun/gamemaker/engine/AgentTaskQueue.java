package com.chengxun.gamemaker.engine;

import com.chengxun.gamemaker.config.SystemConstants;
import com.chengxun.gamemaker.model.TaskAssignment;
import com.chengxun.gamemaker.web.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Agent任务队列
 * 支持任务优先级、重试机制和任务状态跟踪
 *
 * 配置项（通过 SystemConfigService 动态读取）：
 * - agent.task-queue-size: 任务队列最大容量
 * - agent.max-retry-count: 任务最大重试次数
 * - agent.history-size: 历史记录保留数量
 *
 * @author chengxun
 * @since 1.0.0
 */
@Component
public class AgentTaskQueue {

    private static final Logger log = LoggerFactory.getLogger(AgentTaskQueue.class);

    /** 默认任务队列大小（配置不存在时使用） */
    private static final int DEFAULT_QUEUE_SIZE = 500;

    /** 默认最大重试次数（配置不存在时使用） */
    private static final int DEFAULT_RETRY_COUNT = 3;

    /** 默认历史记录大小（配置不存在时使用） */
    private static final int DEFAULT_HISTORY_SIZE = 1000;

    /** 任务队列：按Agent分组，支持优先级 */
    private final ConcurrentHashMap<String, PriorityBlockingQueue<TaskAssignment>> taskQueues = new ConcurrentHashMap<>();

    /** 任务处理器 */
    private final ConcurrentHashMap<String, Consumer<TaskAssignment>> taskHandlers = new ConcurrentHashMap<>();

    /** 任务历史 */
    private final ConcurrentHashMap<String, TaskAssignment> taskHistory = new ConcurrentHashMap<>();

    /** 线程池 */
    private final ExecutorService executorService = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName("agent-task-processor-" + t.getId());
        return t;
    });

    /** 配置服务 */
    private final SystemConfigService configService;

    public AgentTaskQueue(SystemConfigService configService) {
        this.configService = configService;
    }

    /**
     * 获取最大队列大小（从配置读取）
     */
    private int getMaxQueueSize() {
        return configService.getInt(SystemConstants.AGENT_TASK_QUEUE_SIZE, DEFAULT_QUEUE_SIZE);
    }

    /**
     * 获取最大重试次数（从配置读取）
     */
    private int getMaxRetryCount() {
        return configService.getInt(SystemConstants.AGENT_MAX_RETRY_COUNT, DEFAULT_RETRY_COUNT);
    }

    /**
     * 获取历史记录最大大小（从配置读取）
     */
    private int getMaxHistorySize() {
        return configService.getInt(SystemConstants.AGENT_HISTORY_SIZE, DEFAULT_HISTORY_SIZE);
    }

    /**
     * 注册Agent的任务处理器
     */
    public void registerHandler(String agentId, Consumer<TaskAssignment> handler) {
        taskHandlers.put(agentId, handler);
        taskQueues.putIfAbsent(agentId, new PriorityBlockingQueue<>(11,
            (a, b) -> Integer.compare(b.getPriorityValue(), a.getPriorityValue())));
        log.info("Task handler registered for agent: {}", agentId);
    }

    /**
     * 注销Agent的任务处理器
     */
    public void unregisterHandler(String agentId) {
        taskHandlers.remove(agentId);
        taskQueues.remove(agentId);
        log.info("Task handler unregistered for agent: {}", agentId);
    }

    /**
     * 提交任务到指定Agent
     */
    public void submitTask(String agentId, TaskAssignment task) {
        if (task.getId() == null) {
            task.setId(java.util.UUID.randomUUID().toString());
        }

        task.setStatus(TaskAssignment.TaskStatus.PENDING);
        task.setCreatedAt(java.time.LocalDateTime.now());
        task.setCreatedAtMs(System.currentTimeMillis());

        PriorityBlockingQueue<TaskAssignment> queue = taskQueues.computeIfAbsent(
            agentId,
            k -> new PriorityBlockingQueue<>(11, (a, b) -> Integer.compare(b.getPriorityValue(), a.getPriorityValue()))
        );

        int maxSize = getMaxQueueSize();
        if (queue.size() >= maxSize) {
            log.warn("Task queue for agent {} is full (size={}/{}), rejecting task", agentId, queue.size(), maxSize);
            task.setStatus(TaskAssignment.TaskStatus.REJECTED);
            return;
        }

        queue.offer(task);
        taskHistory.put(task.getId(), task);

        log.info("Task {} submitted to agent {} with priority {}", task.getId(), agentId, task.getPriority());

        // 异步处理任务
        processTaskAsync(agentId);
    }

    /**
     * 异步处理任务
     */
    private void processTaskAsync(String agentId) {
        Consumer<TaskAssignment> handler = taskHandlers.get(agentId);
        if (handler == null) {
            log.debug("No handler registered for agent: {}", agentId);
            return;
        }

        executorService.submit(() -> {
            PriorityBlockingQueue<TaskAssignment> queue = taskQueues.get(agentId);
            if (queue == null) return;

            TaskAssignment task;
            while ((task = queue.poll()) != null) {
                try {
                    task.setStatus(TaskAssignment.TaskStatus.PROCESSING);
                    task.setStartedAt(System.currentTimeMillis());

                    handler.accept(task);

                    task.setStatus(TaskAssignment.TaskStatus.COMPLETED);
                    task.setCompletedAt(java.time.LocalDateTime.now());
                    task.setCompletedAtMs(System.currentTimeMillis());
                    log.info("Task {} completed by agent {}", task.getId(), agentId);
                } catch (Exception e) {
                    log.error("Error processing task {} for agent {}: {}",
                        task.getId(), agentId, e.getMessage());
                    handleTaskError(task, agentId, e);
                }
            }
        });
    }

    /**
     * 处理任务错误（支持重试）
     */
    private void handleTaskError(TaskAssignment task, String agentId, Exception error) {
        int retryCount = task.getRetryCount() != null ? task.getRetryCount() : 0;
        int maxRetry = getMaxRetryCount();

        if (retryCount < maxRetry) {
            task.setRetryCount(retryCount + 1);
            task.setStatus(TaskAssignment.TaskStatus.RETRYING);
            log.info("Retrying task {} (attempt {}/{})", task.getId(), retryCount + 1, maxRetry);

            // 延迟重试
            executorService.submit(() -> {
                try {
                    Thread.sleep(2000 * retryCount); // 递增延迟
                    submitTask(agentId, task);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        } else {
            task.setStatus(TaskAssignment.TaskStatus.FAILED);
            task.setError(error.getMessage());
            task.setCompletedAt(java.time.LocalDateTime.now());
            task.setCompletedAtMs(System.currentTimeMillis());
            log.error("Task {} failed after {} retries", task.getId(), maxRetry);
        }
    }

    /**
     * 获取Agent的待处理任务数量
     */
    public int getPendingTaskCount(String agentId) {
        PriorityBlockingQueue<TaskAssignment> queue = taskQueues.get(agentId);
        return queue != null ? queue.size() : 0;
    }

    /**
     * 获取任务信息
     */
    public TaskAssignment getTaskById(String taskId) {
        return taskHistory.get(taskId);
    }

    /**
     * 取消任务
     */
    public boolean cancelTask(String taskId) {
        TaskAssignment task = taskHistory.get(taskId);
        if (task != null && task.getStatus() == TaskAssignment.TaskStatus.PENDING) {
            task.setStatus(TaskAssignment.TaskStatus.CANCELLED);
            return true;
        }
        return false;
    }

    /**
     * 清空Agent的任务队列
     */
    public void clearQueue(String agentId) {
        PriorityBlockingQueue<TaskAssignment> queue = taskQueues.get(agentId);
        if (queue != null) {
            queue.clear();
        }
    }

    /**
     * 定期清理过期的任务历史，防止内存无限增长
     * 每10分钟执行一次，保留最近的MAX_HISTORY_SIZE条记录
     */
    @Scheduled(fixedRate = 600000)
    public void cleanupTaskHistory() {
        int maxSize = getMaxHistorySize();
        if (taskHistory.size() > maxSize) {
            int toRemove = taskHistory.size() - maxSize;
            taskHistory.entrySet().stream()
                .sorted((a, b) -> {
                    long timeA = a.getValue().getCreatedAtMs() != null ? a.getValue().getCreatedAtMs() : 0;
                    long timeB = b.getValue().getCreatedAtMs() != null ? b.getValue().getCreatedAtMs() : 0;
                    return Long.compare(timeA, timeB);
                })
                .limit(toRemove)
                .forEach(entry -> taskHistory.remove(entry.getKey()));
            log.info("Cleaned up {} old task history entries", toRemove);
        }
    }

    /**
     * 关闭任务队列
     */
    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("Task queue shutdown");
    }
}
