package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.model.GameProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 文档归档服务
 * 在里程碑完成、任务完成、版本迭代完成等事件发生时，将对应的文档进行归档
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
public class DocumentArchiveService {

    private static final Logger log = LoggerFactory.getLogger(DocumentArchiveService.class);

    /** 归档目录名 */
    private static final String ARCHIVE_DIR = ".archive";

    /** 时间格式化 */
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Autowired
    private ProjectManager projectManager;

    /**
     * 归档里程碑相关的文档
     * 当里程碑完成时调用，将该里程碑相关的报告文档移动到归档目录
     *
     * @param projectId 项目ID
     * @param milestoneTitle 里程碑标题
     */
    public void archiveMilestoneDocuments(String projectId, String milestoneTitle) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null || project.getWorkDir() == null) return;

        Path workDir = Path.of(project.getWorkDir());
        Path archiveDir = workDir.resolve(ARCHIVE_DIR).resolve("milestones");

        // 查找与里程碑相关的文档
        List<Path> relatedDocs = findRelatedDocuments(workDir, milestoneTitle);
        if (relatedDocs.isEmpty()) {
            log.debug("里程碑 [{}] 没有相关文档需要归档", milestoneTitle);
            return;
        }

        // 创建归档目录
        String timestamp = LocalDateTime.now().format(TIME_FMT);
        Path milestoneArchiveDir = archiveDir.resolve(sanitizeFileName(milestoneTitle) + "_" + timestamp);

        // 归档文档
        for (Path doc : relatedDocs) {
            archiveFile(doc, milestoneArchiveDir);
        }

        log.info("里程碑 [{}] 文档已归档: {} 个文件 -> {}", milestoneTitle, relatedDocs.size(), milestoneArchiveDir);
    }

    /**
     * 归档版本迭代相关的文档
     * 当版本迭代完成时调用，将当前版本的所有报告文档移动到归档目录
     *
     * @param projectId 项目ID
     * @param version 版本号
     */
    public void archiveVersionDocuments(String projectId, String version) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null || project.getWorkDir() == null) return;

        Path workDir = Path.of(project.getWorkDir());
        Path archiveDir = workDir.resolve(ARCHIVE_DIR).resolve("versions");

        // 查找所有报告文档
        List<Path> reportDocs = findReportDocuments(workDir);
        if (reportDocs.isEmpty()) {
            log.debug("版本 [{}] 没有报告文档需要归档", version);
            return;
        }

        // 创建归档目录
        String timestamp = LocalDateTime.now().format(TIME_FMT);
        Path versionArchiveDir = archiveDir.resolve("v" + sanitizeFileName(version) + "_" + timestamp);

        // 归档文档
        for (Path doc : reportDocs) {
            archiveFile(doc, versionArchiveDir);
        }

        log.info("版本 [{}] 文档已归档: {} 个文件 -> {}", version, reportDocs.size(), versionArchiveDir);
    }

    /**
     * 归档任务相关的文档
     * 当任务完成时调用，将该任务相关的文档移动到归档目录
     *
     * @param projectId 项目ID
     * @param taskTitle 任务标题
     */
    public void archiveTaskDocuments(String projectId, String taskTitle) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null || project.getWorkDir() == null) return;

        Path workDir = Path.of(project.getWorkDir());
        Path archiveDir = workDir.resolve(ARCHIVE_DIR).resolve("tasks");

        // 查找与任务相关的文档
        List<Path> relatedDocs = findRelatedDocuments(workDir, taskTitle);
        if (relatedDocs.isEmpty()) {
            log.debug("任务 [{}] 没有相关文档需要归档", taskTitle);
            return;
        }

        // 创建归档目录
        String timestamp = LocalDateTime.now().format(TIME_FMT);
        Path taskArchiveDir = archiveDir.resolve(sanitizeFileName(taskTitle) + "_" + timestamp);

        // 归档文档
        for (Path doc : relatedDocs) {
            archiveFile(doc, taskArchiveDir);
        }

        log.info("任务 [{}] 文档已归档: {} 个文件 -> {}", taskTitle, relatedDocs.size(), taskArchiveDir);
    }

    /**
     * 归档所有旧的报告文档
     * 在版本迭代开始时调用，清理所有旧的报告文档
     *
     * @param projectId 项目ID
     * @return 归档的文件数量
     */
    public int archiveAllReports(String projectId) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null || project.getWorkDir() == null) return 0;

        Path workDir = Path.of(project.getWorkDir());
        Path archiveDir = workDir.resolve(ARCHIVE_DIR).resolve("all-reports");

        // 查找所有报告文档
        List<Path> reportDocs = findReportDocuments(workDir);
        if (reportDocs.isEmpty()) {
            log.debug("没有报告文档需要归档");
            return 0;
        }

        // 创建归档目录
        String timestamp = LocalDateTime.now().format(TIME_FMT);
        Path allReportsArchiveDir = archiveDir.resolve("archive_" + timestamp);

        // 归档文档
        int count = 0;
        for (Path doc : reportDocs) {
            if (archiveFile(doc, allReportsArchiveDir)) {
                count++;
            }
        }

        log.info("所有报告文档已归档: {} 个文件 -> {}", count, allReportsArchiveDir);
        return count;
    }

    /**
     * 查找与指定关键词相关的文档
     */
    private List<Path> findRelatedDocuments(Path workDir, String keyword) {
        String normalizedKeyword = keyword.toLowerCase().replaceAll("\\s+", "-");

        try (Stream<Path> files = Files.list(workDir)) {
            return files
                .filter(Files::isRegularFile)
                .filter(p -> {
                    String fileName = p.getFileName().toString().toLowerCase();
                    return fileName.endsWith(".md") &&
                        (fileName.contains(normalizedKeyword) ||
                         fileName.contains(keyword.toLowerCase().replaceAll("\\s+", "_")));
                })
                .collect(Collectors.toList());
        } catch (IOException e) {
            log.warn("查找相关文档失败: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 查找所有报告文档
     * 匹配常见的报告文件命名模式
     */
    private List<Path> findReportDocuments(Path workDir) {
        // 报告文件的特征后缀
        String[] reportSuffixes = {
            "-REPORT.md", "-SUMMARY.md", "-PLAN.md", "-CHECKLIST.md",
            "_REPORT.md", "_SUMMARY.md", "_PLAN.md", "_CHECKLIST.md",
            "-report.md", "-summary.md", "-plan.md", "-checklist.md",
            "_report.md", "_summary.md", "_plan.md", "_checklist.md"
        };

        // 报告文件的特征前缀
        String[] reportPrefixes = {
            "MILESTONE-", "ITERATION-", "VERSION-", "RELEASE-",
            "PRE-RELEASE-", "FINAL-", "TEST-", "QUALITY-",
            "PROGRESS-", "DELIVERY-", "REGRESSION-", "COMPREHENSIVE-",
            "V1.", "V2.", "V3."
        };

        try (Stream<Path> files = Files.list(workDir)) {
            return files
                .filter(Files::isRegularFile)
                .filter(p -> {
                    String fileName = p.getFileName().toString();
                    if (!fileName.endsWith(".md")) return false;

                    // 检查是否匹配报告后缀
                    for (String suffix : reportSuffixes) {
                        if (fileName.endsWith(suffix)) return true;
                    }

                    // 检查是否匹配报告前缀
                    for (String prefix : reportPrefixes) {
                        if (fileName.startsWith(prefix)) return true;
                    }

                    return false;
                })
                .collect(Collectors.toList());
        } catch (IOException e) {
            log.warn("查找报告文档失败: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 归档单个文件
     *
     * @param sourceFile 源文件
     * @param archiveDir 归档目录
     * @return 是否成功
     */
    private boolean archiveFile(Path sourceFile, Path archiveDir) {
        try {
            // 创建归档目录
            Files.createDirectories(archiveDir);

            // 移动文件
            Path targetFile = archiveDir.resolve(sourceFile.getFileName());
            Files.move(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);

            log.debug("文件已归档: {} -> {}", sourceFile.getFileName(), archiveDir);
            return true;
        } catch (IOException e) {
            log.warn("归档文件失败: {} - {}", sourceFile.getFileName(), e.getMessage());
            return false;
        }
    }

    /**
     * 清理文件名中的特殊字符
     */
    private String sanitizeFileName(String name) {
        if (name == null) return "unknown";
        return name.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5\\-_]", "_")
            .replaceAll("_+", "_")
            .replaceAll("^_|_$", "");
    }
}
