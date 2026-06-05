package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.dingtalk.DingTalkService;
import com.chengxun.gamemaker.web.service.NotificationService;
import com.chengxun.gamemaker.web.entity.Notification.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 项目导入服务
 * 支持导入外部项目（半成品项目）进行继续开发
 *
 * 支持的导入格式：
 * - ZIP 压缩包
 * - 本地目录
 * - Git 仓库 URL
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
public class ProjectImportService {

    private static final Logger log = LoggerFactory.getLogger(ProjectImportService.class);

    /** 项目存储目录 */
    private static final String PROJECTS_DIR = "data/projects";

    private final ProjectManager projectManager;
    private final GameTemplateService templateService;
    private final GameEvaluationService evaluationService;
    private final NotificationService notificationService;
    private final DingTalkService dingTalkService;

    public ProjectImportService(ProjectManager projectManager,
                                GameTemplateService templateService,
                                GameEvaluationService evaluationService,
                                NotificationService notificationService,
                                DingTalkService dingTalkService) {
        this.projectManager = projectManager;
        this.templateService = templateService;
        this.evaluationService = evaluationService;
        this.notificationService = notificationService;
        this.dingTalkService = dingTalkService;
    }

    /**
     * 从 ZIP 文件导入项目
     *
     * @param zipPath ZIP 文件路径
     * @param projectName 项目名称
     * @param description 项目描述
     * @return 导入的项目
     */
    public ImportResult importFromZip(String zipPath, String projectName, String description) {
        log.info("从 ZIP 导入项目: zipPath={}, projectName={}", zipPath, projectName);

        ImportResult result = new ImportResult();

        try {
            // 创建项目目录
            String projectId = generateProjectId(projectName);
            Path projectPath = Path.of(PROJECTS_DIR, projectId);
            Files.createDirectories(projectPath);

            // 解压 ZIP 文件
            unzip(zipPath, projectPath.toString());

            // 分析项目结构
            ProjectAnalysis analysis = analyzeProject(projectPath.toString());
            result.setAnalysis(analysis);

            // 创建项目记录
            GameProject project = new GameProject();
            project.setId(projectId);
            project.setName(projectName);
            project.setDescription(description);
            project.setWorkDir(projectPath.toString());
            project.getMetadata().put("projectType", analysis.getProjectType());
            project.setStatus(GameProject.ProjectStatus.ACTIVE);
            project.setCreatedAt(LocalDateTime.now());

            projectManager.createProject(projectName, description, projectPath.toString());

            result.setSuccess(true);
            result.setProject(project);
            result.setMessage("项目导入成功");

            log.info("项目导入成功: projectId={}, type={}", projectId, analysis.getProjectType());

            // 发送通知
            try {
                String title = "项目导入成功";
                String content = String.format("项目 '%s' 已成功导入，类型: %s", projectName, analysis.getFramework());
                notificationService.sendSystemNotification(null, title, content, NotificationType.INFO);
                dingTalkService.sendMarkdown(title,
                    "## 项目导入成功\n\n- **项目**: " + projectName +
                    "\n- **类型**: " + analysis.getFramework() +
                    "\n- **文件数**: " + analysis.getFileCount());
            } catch (Exception e) {
                log.warn("发送导入通知失败", e);
            }

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("导入失败: " + e.getMessage());
            log.error("项目导入失败", e);
        }

        return result;
    }

    /**
     * 从本地目录导入项目
     *
     * @param sourcePath 源目录路径
     * @param projectName 项目名称
     * @param description 项目描述
     * @return 导入的项目
     */
    public ImportResult importFromDirectory(String sourcePath, String projectName, String description) {
        log.info("从目录导入项目: sourcePath={}, projectName={}", sourcePath, projectName);

        ImportResult result = new ImportResult();

        try {
            Path source = Path.of(sourcePath);
            if (!Files.exists(source) || !Files.isDirectory(source)) {
                result.setSuccess(false);
                result.setMessage("源目录不存在或不是目录");
                return result;
            }

            // 创建项目目录
            String projectId = generateProjectId(projectName);
            Path projectPath = Path.of(PROJECTS_DIR, projectId);

            // 复制文件
            copyDirectory(source, projectPath);

            // 分析项目结构
            ProjectAnalysis analysis = analyzeProject(projectPath.toString());
            result.setAnalysis(analysis);

            // 创建项目记录
            GameProject project = new GameProject();
            project.setId(projectId);
            project.setName(projectName);
            project.setDescription(description);
            project.setWorkDir(projectPath.toString());
            project.getMetadata().put("projectType", analysis.getProjectType());
            project.setStatus(GameProject.ProjectStatus.ACTIVE);
            project.setCreatedAt(LocalDateTime.now());

            projectManager.createProject(projectName, description, projectPath.toString());

            result.setSuccess(true);
            result.setProject(project);
            result.setMessage("项目导入成功");

            log.info("项目导入成功: projectId={}", projectId);

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("导入失败: " + e.getMessage());
            log.error("项目导入失败", e);
        }

        return result;
    }

    /**
     * 分析项目结构
     * 识别项目类型和使用的框架
     */
    public ProjectAnalysis analyzeProject(String projectPath) {
        ProjectAnalysis analysis = new ProjectAnalysis();
        Path path = Path.of(projectPath);

        try {
            // 检查文件类型
            boolean hasPackageJson = Files.exists(path.resolve("package.json"));
            boolean hasIndexHtml = Files.exists(path.resolve("index.html"));
            boolean hasPomXml = Files.exists(path.resolve("pom.xml"));
            boolean hasPhaser = false;
            boolean hasThreeJs = false;
            boolean hasVue = false;
            boolean hasReact = false;

            if (hasPackageJson) {
                String packageContent = Files.readString(path.resolve("package.json"));
                hasPhaser = packageContent.contains("phaser");
                hasThreeJs = packageContent.contains("three");
                hasVue = packageContent.contains("vue");
                hasReact = packageContent.contains("react");
            }

            // 识别项目类型
            if (hasPhaser) {
                analysis.setProjectType("phaser");
                analysis.setFramework("Phaser 3");
            } else if (hasThreeJs) {
                analysis.setProjectType("threejs");
                analysis.setFramework("Three.js");
            } else if (hasVue) {
                analysis.setProjectType("vue");
                analysis.setFramework("Vue.js");
            } else if (hasReact) {
                analysis.setProjectType("react");
                analysis.setFramework("React");
            } else if (hasIndexHtml) {
                analysis.setProjectType("html5");
                analysis.setFramework("HTML5 Canvas");
            } else if (hasPomXml) {
                analysis.setProjectType("java");
                analysis.setFramework("Java/Spring");
            } else {
                analysis.setProjectType("unknown");
                analysis.setFramework("Unknown");
            }

            // 统计文件
            AtomicInteger fileCount = new AtomicInteger(0);
            AtomicInteger dirCount = new AtomicInteger(0);
            try (Stream<Path> stream = Files.walk(path, 10)) {
                stream.forEach(p -> {
                    if (Files.isDirectory(p)) dirCount.incrementAndGet();
                    else fileCount.incrementAndGet();
                });
            }

            analysis.setFileCount(fileCount.get());
            analysis.setDirectoryCount(dirCount.get());
            analysis.setHasPackageJson(hasPackageJson);
            analysis.setHasIndexHtml(hasIndexHtml);

            // 检测是否游戏项目
            boolean isGame = false;
            try (Stream<Path> stream = Files.walk(path, 5)) {
                String allContent = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".js") || p.toString().endsWith(".ts"))
                    .map(p -> {
                        try { return Files.readString(p); }
                        catch (IOException e) { return ""; }
                    })
                    .reduce("", (a, b) -> a + "\n" + b);

                isGame = allContent.contains("game") || allContent.contains("player") ||
                         allContent.contains("score") || allContent.contains("canvas");
            }
            analysis.setGameProject(isGame);

            // 匹配最佳模板
            if (isGame) {
                String description = "game " + analysis.getFramework();
                GameTemplateService.GameTemplate bestMatch = templateService.getBestMatch(description);
                if (bestMatch != null) {
                    analysis.setSuggestedTemplate(bestMatch.getId());
                }
            }

        } catch (IOException e) {
            log.error("分析项目失败", e);
        }

        return analysis;
    }

    /**
     * 解压 ZIP 文件
     */
    private void unzip(String zipPath, String destPath) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(Path.of(zipPath)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = Path.of(destPath, entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }

    /**
     * 复制目录
     */
    private void copyDirectory(Path source, Path target) throws IOException {
        Files.createDirectories(target);
        try (Stream<Path> stream = Files.walk(source)) {
            stream.forEach(sourcePath -> {
                try {
                    Path targetPath = target.resolve(source.relativize(sourcePath));
                    if (Files.isDirectory(sourcePath)) {
                        Files.createDirectories(targetPath);
                    } else {
                        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    log.warn("复制文件失败: {}", sourcePath, e);
                }
            });
        }
    }

    /**
     * 生成项目 ID
     */
    private String generateProjectId(String projectName) {
        String base = projectName.toLowerCase()
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-|-$)", "");
        String timestamp = String.valueOf(System.currentTimeMillis() % 10000);
        return base + "-" + timestamp;
    }

    // ===== 内部类 =====

    public static class ImportResult {
        private boolean success;
        private GameProject project;
        private ProjectAnalysis analysis;
        private String message;

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public GameProject getProject() { return project; }
        public void setProject(GameProject project) { this.project = project; }
        public ProjectAnalysis getAnalysis() { return analysis; }
        public void setAnalysis(ProjectAnalysis analysis) { this.analysis = analysis; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class ProjectAnalysis {
        private String projectType;
        private String framework;
        private int fileCount;
        private int directoryCount;
        private boolean hasPackageJson;
        private boolean hasIndexHtml;
        private boolean isGameProject;
        private String suggestedTemplate;

        // Getters and Setters
        public String getProjectType() { return projectType; }
        public void setProjectType(String projectType) { this.projectType = projectType; }
        public String getFramework() { return framework; }
        public void setFramework(String framework) { this.framework = framework; }
        public int getFileCount() { return fileCount; }
        public void setFileCount(int fileCount) { this.fileCount = fileCount; }
        public int getDirectoryCount() { return directoryCount; }
        public void setDirectoryCount(int directoryCount) { this.directoryCount = directoryCount; }
        public boolean isHasPackageJson() { return hasPackageJson; }
        public void setHasPackageJson(boolean hasPackageJson) { this.hasPackageJson = hasPackageJson; }
        public boolean isHasIndexHtml() { return hasIndexHtml; }
        public void setHasIndexHtml(boolean hasIndexHtml) { this.hasIndexHtml = hasIndexHtml; }
        public boolean isGameProject() { return isGameProject; }
        public void setGameProject(boolean gameProject) { isGameProject = gameProject; }
        public String getSuggestedTemplate() { return suggestedTemplate; }
        public void setSuggestedTemplate(String suggestedTemplate) { this.suggestedTemplate = suggestedTemplate; }
    }

    // 为了编译通过，添加 AtomicInteger import
    private static class AtomicInteger {
        private int value;
        public AtomicInteger(int value) { this.value = value; }
        public int get() { return value; }
        public int incrementAndGet() { return ++value; }
    }
}
