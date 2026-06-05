package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.web.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.sun.management.OperatingSystemMXBean;
import javax.sql.DataSource;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.ThreadMXBean;
import java.lang.management.ThreadInfo;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统自检服务
 * 提供系统级别的健康检查和诊断功能
 *
 * 检查项目：
 * - 数据库连接
 * - 缓存状态
 * - 磁盘空间
 * - 内存使用
 * - 线程池状态
 * - Agent 状态
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
public class SystemDiagnosticService {

    private static final Logger log = LoggerFactory.getLogger(SystemDiagnosticService.class);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private AgentManager agentManager;

    @Autowired
    private UserRepository userRepository;

    /** 最近一次诊断结果缓存 */
    private Map<String, Object> lastDiagnosticResult = new ConcurrentHashMap<>();

    /** 最近一次诊断时间 */
    private LocalDateTime lastDiagnosticTime;

    /**
     * 执行完整系统自检
     *
     * @return 诊断结果
     */
    public Map<String, Object> runFullDiagnostic() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("timestamp", LocalDateTime.now());
        result.put("overallStatus", "UP");

        // 各项检查
        Map<String, Object> checks = new LinkedHashMap<>();

        checks.put("cpu", checkCpu());
        checks.put("memory", checkMemory());
        checks.put("disk", checkDisk());
        checks.put("database", checkDatabase());
        checks.put("threads", checkThreads());
        checks.put("agents", checkAgents());
        checks.put("users", checkUsers());

        result.put("checks", checks);

        // 计算总体状态
        boolean allUp = checks.values().stream()
            .allMatch(check -> "UP".equals(((Map<?, ?>) check).get("status")));
        result.put("overallStatus", allUp ? "UP" : "DEGRADED");

        // 缓存结果
        lastDiagnosticResult = result;
        lastDiagnosticTime = LocalDateTime.now();

        return result;
    }

    /**
     * 获取最近一次诊断结果
     *
     * @return 诊断结果
     */
    public Map<String, Object> getLastDiagnosticResult() {
        if (lastDiagnosticResult.isEmpty()) {
            return runFullDiagnostic();
        }
        return lastDiagnosticResult;
    }

    /**
     * 检查 CPU 使用情况
     */
    private Map<String, Object> checkCpu() {
        Map<String, Object> check = new LinkedHashMap<>();
        check.put("name", "CPU 使用");

        try {
            OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

            int availableProcessors = osBean.getAvailableProcessors();
            double systemCpuLoad = osBean.getSystemCpuLoad() * 100;
            double processCpuLoad = osBean.getProcessCpuLoad() * 100;

            // 系统负载
            double systemLoadAverage = osBean.getSystemLoadAverage();

            check.put("status", systemCpuLoad < 90 ? "UP" : "WARNING");
            check.put("availableProcessors", availableProcessors);
            check.put("systemCpuPercent", Math.round(systemCpuLoad * 100.0) / 100.0);
            check.put("processCpuPercent", Math.round(processCpuLoad * 100.0) / 100.0);
            check.put("systemLoadAverage", Math.round(systemLoadAverage * 100.0) / 100.0);

            // 运行时间
            long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
            Duration uptime = Duration.ofMillis(uptimeMs);
            check.put("uptimeMs", uptimeMs);
            check.put("uptimeFormatted", formatUptime(uptime));

        } catch (Exception e) {
            check.put("status", "DOWN");
            check.put("error", e.getMessage());
            log.error("CPU 检查失败", e);
        }

        return check;
    }

    /**
     * 格式化运行时间
     */
    private String formatUptime(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        if (days > 0) {
            return String.format("%d天 %d小时 %d分钟", days, hours, minutes);
        } else if (hours > 0) {
            return String.format("%d小时 %d分钟 %d秒", hours, minutes, seconds);
        } else {
            return String.format("%d分钟 %d秒", minutes, seconds);
        }
    }

    /**
     * 检查数据库连接
     */
    private Map<String, Object> checkDatabase() {
        Map<String, Object> check = new LinkedHashMap<>();
        check.put("name", "数据库连接");

        try (Connection conn = dataSource.getConnection()) {
            boolean valid = conn.isValid(5);
            check.put("status", valid ? "UP" : "DOWN");

            // 获取数据库信息
            check.put("database", conn.getMetaData().getDatabaseProductName());
            check.put("version", conn.getMetaData().getDatabaseProductVersion());
            check.put("url", conn.getMetaData().getURL());

            // 执行简单查询验证
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT 1")) {
                check.put("queryTest", rs.next() ? "PASS" : "FAIL");
            }

        } catch (Exception e) {
            check.put("status", "DOWN");
            check.put("error", e.getMessage());
            log.error("数据库健康检查失败", e);
        }

        return check;
    }

    /**
     * 检查内存使用
     */
    private Map<String, Object> checkMemory() {
        Map<String, Object> check = new LinkedHashMap<>();
        check.put("name", "内存使用");

        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            Runtime runtime = Runtime.getRuntime();

            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;

            double usagePercent = (double) usedMemory / maxMemory * 100;

            check.put("status", usagePercent < 90 ? "UP" : "WARNING");
            check.put("maxMemoryMB", maxMemory / 1024 / 1024);
            check.put("totalMemoryMB", totalMemory / 1024 / 1024);
            check.put("usedMemoryMB", usedMemory / 1024 / 1024);
            check.put("freeMemoryMB", freeMemory / 1024 / 1024);
            check.put("usagePercent", Math.round(usagePercent * 100.0) / 100.0);

            // 堆内存
            check.put("heapUsedMB", memoryBean.getHeapMemoryUsage().getUsed() / 1024 / 1024);
            check.put("heapMaxMB", memoryBean.getHeapMemoryUsage().getMax() / 1024 / 1024);

        } catch (Exception e) {
            check.put("status", "DOWN");
            check.put("error", e.getMessage());
            log.error("内存检查失败", e);
        }

        return check;
    }

    /**
     * 检查磁盘空间
     */
    private Map<String, Object> checkDisk() {
        Map<String, Object> check = new LinkedHashMap<>();
        check.put("name", "磁盘空间");

        try {
            File workDir = new File(".");
            long totalSpace = workDir.getTotalSpace();
            long freeSpace = workDir.getFreeSpace();
            long usableSpace = workDir.getUsableSpace();
            long usedSpace = totalSpace - freeSpace;

            double usagePercent = (double) usedSpace / totalSpace * 100;

            check.put("status", usagePercent < 90 ? "UP" : "WARNING");
            check.put("totalSpaceGB", Math.round((double) totalSpace / 1024 / 1024 / 1024 * 100.0) / 100.0);
            check.put("freeSpaceGB", Math.round((double) freeSpace / 1024 / 1024 / 1024 * 100.0) / 100.0);
            check.put("usableSpaceGB", Math.round((double) usableSpace / 1024 / 1024 / 1024 * 100.0) / 100.0);
            check.put("usagePercent", Math.round(usagePercent * 100.0) / 100.0);
            check.put("workDir", workDir.getAbsolutePath());

        } catch (Exception e) {
            check.put("status", "DOWN");
            check.put("error", e.getMessage());
            log.error("磁盘检查失败", e);
        }

        return check;
    }

    /**
     * 检查线程状态
     */
    private Map<String, Object> checkThreads() {
        Map<String, Object> check = new LinkedHashMap<>();
        check.put("name", "线程状态");

        try {
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

            int threadCount = threadBean.getThreadCount();
            int daemonThreadCount = threadBean.getDaemonThreadCount();
            int peakThreadCount = threadBean.getPeakThreadCount();
            long totalStartedThreadCount = threadBean.getTotalStartedThreadCount();

            check.put("status", threadCount < 500 ? "UP" : "WARNING");
            check.put("threadCount", threadCount);
            check.put("daemonThreadCount", daemonThreadCount);
            check.put("peakThreadCount", peakThreadCount);
            check.put("totalStartedThreadCount", totalStartedThreadCount);

        } catch (Exception e) {
            check.put("status", "DOWN");
            check.put("error", e.getMessage());
            log.error("线程检查失败", e);
        }

        return check;
    }

    /**
     * 检查 Agent 状态
     */
    private Map<String, Object> checkAgents() {
        Map<String, Object> check = new LinkedHashMap<>();
        check.put("name", "Agent 状态");

        try {
            var agents = agentManager.getAllAgents();

            int total = agents.size();
            int alive = (int) agents.stream().filter(a -> a.isAlive()).count();
            int busy = (int) agents.stream().filter(a -> a.isBusy()).count();

            check.put("status", total > 0 ? "UP" : "WARNING");
            check.put("totalAgents", total);
            check.put("aliveAgents", alive);
            check.put("busyAgents", busy);
            check.put("idleAgents", alive - busy);

        } catch (Exception e) {
            check.put("status", "DOWN");
            check.put("error", e.getMessage());
            log.error("Agent 检查失败", e);
        }

        return check;
    }

    /**
     * 检查用户状态
     */
    private Map<String, Object> checkUsers() {
        Map<String, Object> check = new LinkedHashMap<>();
        check.put("name", "用户状态");

        try {
            long totalUsers = userRepository.count();
            long pendingUsers = userRepository.countByStatus(
                com.chengxun.gamemaker.web.entity.User.UserStatus.PENDING);

            check.put("status", "UP");
            check.put("totalUsers", totalUsers);
            check.put("pendingUsers", pendingUsers);

        } catch (Exception e) {
            check.put("status", "DOWN");
            check.put("error", e.getMessage());
            log.error("用户检查失败", e);
        }

        return check;
    }

    /**
     * 定期执行自检（每 5 分钟）
     */
    @Scheduled(fixedRate = 300000)
    public void scheduledDiagnostic() {
        try {
            runFullDiagnostic();
            log.debug("系统自检完成，状态: {}", lastDiagnosticResult.get("overallStatus"));
        } catch (Exception e) {
            log.error("系统自检失败", e);
        }
    }

    // ===== 详细数据接口 =====

    /**
     * 获取 CPU 详细信息
     * 包含各处理器负载、系统属性等
     */
    public Map<String, Object> getCpuDetails() {
        Map<String, Object> details = new LinkedHashMap<>();
        try {
            OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            Runtime runtime = Runtime.getRuntime();

            // 基本信息
            details.put("osName", System.getProperty("os.name"));
            details.put("osArch", System.getProperty("os.arch"));
            details.put("osVersion", System.getProperty("os.version"));
            details.put("availableProcessors", osBean.getAvailableProcessors());

            // CPU 负载
            details.put("systemCpuLoad", Math.round(osBean.getSystemCpuLoad() * 10000.0) / 100.0);
            details.put("processCpuLoad", Math.round(osBean.getProcessCpuLoad() * 10000.0) / 100.0);
            details.put("systemLoadAverage", osBean.getSystemLoadAverage());

            // 进程信息
            details.put("pid", ProcessHandle.current().pid());
            details.put("processCpuTime", osBean.getProcessCpuTime() / 1_000_000); // ms
            details.put("committedVirtualMemory", osBean.getCommittedVirtualMemorySize() / 1024 / 1024); // MB

            // JVM 信息
            details.put("javaVersion", System.getProperty("java.version"));
            details.put("javaVendor", System.getProperty("java.vendor"));
            details.put("jvmName", System.getProperty("java.vm.name"));
            details.put("jvmVersion", System.getProperty("java.vm.version"));

            // 运行时间
            long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
            details.put("uptimeMs", uptimeMs);
            details.put("uptimeFormatted", formatUptime(Duration.ofMillis(uptimeMs)));

            // 启动参数
            List<String> inputArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
            details.put("jvmArgs", inputArgs);

        } catch (Exception e) {
            details.put("error", e.getMessage());
            log.error("CPU 详细信息获取失败", e);
        }
        return details;
    }

    /**
     * 获取内存详细信息
     * 包含各内存池使用情况
     */
    public Map<String, Object> getMemoryDetails() {
        Map<String, Object> details = new LinkedHashMap<>();
        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            Runtime runtime = Runtime.getRuntime();

            // 堆内存
            Map<String, Object> heap = new LinkedHashMap<>();
            heap.put("used", memoryBean.getHeapMemoryUsage().getUsed() / 1024 / 1024);
            heap.put("committed", memoryBean.getHeapMemoryUsage().getCommitted() / 1024 / 1024);
            heap.put("max", memoryBean.getHeapMemoryUsage().getMax() / 1024 / 1024);
            details.put("heap", heap);

            // 非堆内存
            Map<String, Object> nonHeap = new LinkedHashMap<>();
            nonHeap.put("used", memoryBean.getNonHeapMemoryUsage().getUsed() / 1024 / 1024);
            nonHeap.put("committed", memoryBean.getNonHeapMemoryUsage().getCommitted() / 1024 / 1024);
            nonHeap.put("max", memoryBean.getNonHeapMemoryUsage().getMax() / 1024 / 1024);
            details.put("nonHeap", nonHeap);

            // 内存池详情
            List<Map<String, Object>> pools = new ArrayList<>();
            for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
                Map<String, Object> poolInfo = new LinkedHashMap<>();
                poolInfo.put("name", pool.getName());
                poolInfo.put("type", pool.getType().name());
                poolInfo.put("used", pool.getUsage().getUsed() / 1024 / 1024);
                poolInfo.put("committed", pool.getUsage().getCommitted() / 1024 / 1024);
                poolInfo.put("max", pool.getUsage().getMax() / 1024 / 1024);
                if (pool.getUsage().getMax() > 0) {
                    poolInfo.put("usagePercent", Math.round((double) pool.getUsage().getUsed() / pool.getUsage().getMax() * 10000.0) / 100.0);
                }
                pools.add(poolInfo);
            }
            details.put("pools", pools);

            // GC 信息
            List<Map<String, Object>> gcInfos = new ArrayList<>();
            for (var gc : ManagementFactory.getGarbageCollectorMXBeans()) {
                Map<String, Object> gcInfo = new LinkedHashMap<>();
                gcInfo.put("name", gc.getName());
                gcInfo.put("collectionCount", gc.getCollectionCount());
                gcInfo.put("collectionTime", gc.getCollectionTime());
                gcInfo.put("memoryPoolNames", gc.getMemoryPoolNames());
                gcInfos.add(gcInfo);
            }
            details.put("gc", gcInfos);

            // 总体内存
            details.put("totalMemoryMB", runtime.totalMemory() / 1024 / 1024);
            details.put("freeMemoryMB", runtime.freeMemory() / 1024 / 1024);
            details.put("maxMemoryMB", runtime.maxMemory() / 1024 / 1024);
            details.put("usedMemoryMB", (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024);

        } catch (Exception e) {
            details.put("error", e.getMessage());
            log.error("内存详细信息获取失败", e);
        }
        return details;
    }

    /**
     * 获取线程详细信息
     * 包含线程列表、状态分布等
     */
    public Map<String, Object> getThreadDetails() {
        Map<String, Object> details = new LinkedHashMap<>();
        try {
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

            // 基本统计
            details.put("threadCount", threadBean.getThreadCount());
            details.put("daemonThreadCount", threadBean.getDaemonThreadCount());
            details.put("peakThreadCount", threadBean.getPeakThreadCount());
            details.put("totalStartedThreadCount", threadBean.getTotalStartedThreadCount());

            // 线程状态分布
            Map<String, Integer> stateCount = new LinkedHashMap<>();
            for (Thread.State state : Thread.State.values()) {
                stateCount.put(state.name(), 0);
            }
            ThreadInfo[] threadInfos = threadBean.getThreadInfo(threadBean.getAllThreadIds(), 0);
            for (ThreadInfo info : threadInfos) {
                if (info != null) {
                    stateCount.merge(info.getThreadState().name(), 1, Integer::sum);
                }
            }
            details.put("stateDistribution", stateCount);

            // 线程列表（前 50 个）
            List<Map<String, Object>> threadList = new ArrayList<>();
            long[] threadIds = threadBean.getAllThreadIds();
            int limit = Math.min(threadIds.length, 50);
            for (int i = 0; i < limit; i++) {
                ThreadInfo info = threadBean.getThreadInfo(threadIds[i], 5); // 包含5层堆栈
                if (info != null) {
                    Map<String, Object> thread = new LinkedHashMap<>();
                    thread.put("id", info.getThreadId());
                    thread.put("name", info.getThreadName());
                    thread.put("state", info.getThreadState().name());
                    thread.put("daemon", isDaemonThread(info.getThreadId()));
                    thread.put("cpuTime", threadBean.getThreadCpuTime(info.getThreadId()) / 1_000_000); // ms
                    thread.put("userTime", threadBean.getThreadUserTime(info.getThreadId()) / 1_000_000); // ms

                    // 堆栈信息
                    StackTraceElement[] stackTrace = info.getStackTrace();
                    if (stackTrace.length > 0) {
                        List<String> stack = new ArrayList<>();
                        for (int j = 0; j < Math.min(stackTrace.length, 5); j++) {
                            stack.add(stackTrace[j].toString());
                        }
                        thread.put("stackTrace", stack);
                    }

                    threadList.add(thread);
                }
            }
            details.put("threads", threadList);
            details.put("totalShown", threadList.size());

        } catch (Exception e) {
            details.put("error", e.getMessage());
            log.error("线程详细信息获取失败", e);
        }
        return details;
    }

    /**
     * 判断线程是否为守护线程
     */
    private boolean isDaemonThread(long threadId) {
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if (thread.getId() == threadId) {
                return thread.isDaemon();
            }
        }
        return false;
    }

    /**
     * 获取磁盘详细信息
     * 包含各挂载点使用情况
     */
    public Map<String, Object> getDiskDetails() {
        Map<String, Object> details = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> drives = new ArrayList<>();
            for (File root : File.listRoots()) {
                Map<String, Object> drive = new LinkedHashMap<>();
                drive.put("path", root.getAbsolutePath());
                drive.put("totalGB", Math.round(root.getTotalSpace() / 1024.0 / 1024 / 1024 * 100.0) / 100.0);
                drive.put("freeGB", Math.round(root.getFreeSpace() / 1024.0 / 1024 / 1024 * 100.0) / 100.0);
                drive.put("usableGB", Math.round(root.getUsableSpace() / 1024.0 / 1024 / 1024 * 100.0) / 100.0);
                long used = root.getTotalSpace() - root.getFreeSpace();
                drive.put("usagePercent", Math.round((double) used / root.getTotalSpace() * 10000.0) / 100.0);
                drives.add(drive);
            }
            details.put("drives", drives);

            // 工作目录信息
            File workDir = new File(".");
            details.put("workDir", workDir.getAbsolutePath());
            details.put("workDirTotalGB", Math.round(workDir.getTotalSpace() / 1024.0 / 1024 / 1024 * 100.0) / 100.0);
            details.put("workDirFreeGB", Math.round(workDir.getFreeSpace() / 1024.0 / 1024 / 1024 * 100.0) / 100.0);

        } catch (Exception e) {
            details.put("error", e.getMessage());
            log.error("磁盘详细信息获取失败", e);
        }
        return details;
    }
}
