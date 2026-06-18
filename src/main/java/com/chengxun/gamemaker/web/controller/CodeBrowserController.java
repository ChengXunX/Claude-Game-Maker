package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.model.GameProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 代码浏览器控制器
 * 提供项目代码文件的浏览、查看和下载功能
 *
 * @author chengxun
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/code-browser")
@PreAuthorize("isAuthenticated()")
public class CodeBrowserController {

    private static final Logger log = LoggerFactory.getLogger(CodeBrowserController.class);

    private final ProjectManager projectManager;

    /** 允许预览的文件扩展名 */
    private static final Set<String> TEXT_EXTENSIONS = Set.of(
        "java", "py", "js", "ts", "jsx", "tsx", "html", "htm", "css", "scss", "less", "sass",
        "xml", "json", "yaml", "yml", "toml", "ini", "properties", "conf", "cfg", "cnf",
        "md", "txt", "log", "csv", "sql", "sh", "bash", "bat", "cmd", "ps1",
        "c", "cpp", "h", "hpp", "cs", "go", "rs", "rb", "php", "swift", "kt",
        "vue", "svelte", "astro", "dart", "lua", "r", "m", "mm",
        "env", "gitignore", "dockerignore", "editorconfig", "prettierrc", "eslintrc",
        "dockerfile", "makefile", "cmake", "gradle", "sbt",
        "svg", "graphql", "gql", "proto", "thrift",
        "lock", "sum", "mod",
        "tf", "hcl", "nomad", "service", "nginx",
        "rst", "asciidoc", "adoc", "tex", "bib",
        "wasm", "wat",
        "sol", "vyper",
        "zig", "nim", "v", "ex", "exs", "erl", "hrl", "clj", "cljs", "hs", "elm"
    );

    /** 需要排除的目录 */
    private static final Set<String> EXCLUDED_DIRS = Set.of(
        ".git", "node_modules", ".idea", ".vscode", "target", "build", "dist",
        "__pycache__", ".cache", ".gradle", ".mvn"
    );

    /** 需要排除的隐藏目录（但不包括 .game-maker） */
    private static final Set<String> EXCLUDED_HIDDEN_PREFIXES = Set.of(
        ".git", ".idea", ".vscode", ".cache", ".gradle", ".mvn"
    );

    public CodeBrowserController(ProjectManager projectManager) {
        this.projectManager = projectManager;
    }

    /**
     * 获取项目的文件树
     */
    @GetMapping("/tree/{projectId}")
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public ResponseEntity<?> getFileTree(@PathVariable String projectId,
                                          @RequestParam(defaultValue = "") String path) {
        try {
            GameProject project = projectManager.getProject(projectId);
            if (project == null) {
                return ResponseEntity.notFound().build();
            }

            Path projectDir = Path.of(project.getWorkDir());
            if (!Files.exists(projectDir)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "项目目录不存在"
                ));
            }

            Path targetDir = path.isEmpty() ? projectDir : projectDir.resolve(path);
            if (!Files.exists(targetDir) || !Files.isDirectory(targetDir)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "目录不存在"
                ));
            }

            // 安全检查：确保路径在项目目录内
            if (!targetDir.normalize().startsWith(projectDir.normalize())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "非法路径"
                ));
            }

            List<Map<String, Object>> fileTree = buildFileTree(targetDir, projectDir);
            return ResponseEntity.ok(fileTree);

        } catch (Exception e) {
            log.error("获取文件树失败: projectId={}", projectId, e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "获取文件树失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取文件内容
     */
    @GetMapping("/content/{projectId}")
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public ResponseEntity<?> getFileContent(@PathVariable String projectId,
                                             @RequestParam String path) {
        try {
            GameProject project = projectManager.getProject(projectId);
            if (project == null) {
                return ResponseEntity.notFound().build();
            }

            Path projectDir = Path.of(project.getWorkDir());
            Path filePath = projectDir.resolve(path);

            // 安全检查
            if (!filePath.normalize().startsWith(projectDir.normalize())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "非法路径"
                ));
            }

            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "文件不存在"
                ));
            }

            // 检查文件大小（限制10MB）
            long fileSize = Files.size(filePath);
            if (fileSize > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "文件过大，请下载查看"
                ));
            }

            // 读取文件内容（未知扩展名也尝试按文本读取）
            String extension = getExtension(path);
            String content;
            try {
                content = Files.readString(filePath);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "无法按文本读取此文件（可能是二进制文件）"
                ));
            }

            // 检测是否为二进制文件（包含大量空字符）
            long nullCount = content.chars().filter(c -> c == 0).limit(1000).count();
            if (nullCount > 10) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "此文件为二进制文件，无法按文本预览"
                ));
            }
            return ResponseEntity.ok(Map.of(
                "success", true,
                "content", content,
                "language", getLanguage(extension),
                "size", fileSize,
                "path", path
            ));

        } catch (IOException e) {
            log.error("读取文件失败: projectId={}, path={}", projectId, path, e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "读取文件失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取文件原始内容（流式，用于图片/视频/音频内联预览）
     * 返回文件二进制流，设置正确的 Content-Type 和 Content-Disposition: inline
     *
     * @param projectId 项目ID
     * @param path 文件路径（相对于项目目录）
     * @return 文件流
     */
    @GetMapping("/raw/{projectId}")
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public ResponseEntity<Resource> getRawFile(@PathVariable String projectId,
                                                @RequestParam String path) {
        try {
            GameProject project = projectManager.getProject(projectId);
            if (project == null) {
                return ResponseEntity.notFound().build();
            }

            Path projectDir = Path.of(project.getWorkDir());
            Path filePath = projectDir.resolve(path);

            // 安全检查
            if (!filePath.normalize().startsWith(projectDir.normalize())) {
                return ResponseEntity.badRequest().build();
            }

            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(filePath.toFile());
            String extension = getExtension(path);
            MediaType mediaType = getMediaType(extension);

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .contentType(mediaType)
                .contentLength(Files.size(filePath))
                .body(resource);

        } catch (Exception e) {
            log.error("获取原始文件失败: projectId={}, path={}", projectId, path, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 下载文件
     */
    @GetMapping("/download/{projectId}")
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public ResponseEntity<?> downloadFile(@PathVariable String projectId,
                                           @RequestParam String path) {
        try {
            GameProject project = projectManager.getProject(projectId);
            if (project == null) {
                return ResponseEntity.notFound().build();
            }

            Path projectDir = Path.of(project.getWorkDir());
            Path filePath = projectDir.resolve(path);

            // 安全检查
            if (!filePath.normalize().startsWith(projectDir.normalize())) {
                return ResponseEntity.badRequest().body("非法路径");
            }

            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(filePath.toFile());
            String fileName = filePath.getFileName().toString();

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(Files.size(filePath))
                .body(resource);

        } catch (Exception e) {
            log.error("下载文件失败: projectId={}, path={}", projectId, path, e);
            return ResponseEntity.internalServerError().body("下载失败: " + e.getMessage());
        }
    }

    /**
     * 构建文件树
     */
    private List<Map<String, Object>> buildFileTree(Path dir, Path rootDir) throws IOException {
        List<Map<String, Object>> result = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            List<Path> entries = new ArrayList<>();
            stream.forEach(entries::add);

            // 排序：目录在前，文件在后，按名称排序
            entries.sort((a, b) -> {
                boolean aDir = Files.isDirectory(a);
                boolean bDir = Files.isDirectory(b);
                if (aDir != bDir) return aDir ? -1 : 1;
                return a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString());
            });

            for (Path entry : entries) {
                String name = entry.getFileName().toString();

                // 跳过排除的目录
                if (EXCLUDED_DIRS.contains(name)) {
                    continue;
                }

                // 跳过特定的隐藏目录（但保留 .game-maker 等项目配置目录）
                if (name.startsWith(".") && Files.isDirectory(entry)) {
                    boolean excluded = false;
                    for (String prefix : EXCLUDED_HIDDEN_PREFIXES) {
                        if (name.startsWith(prefix)) {
                            excluded = true;
                            break;
                        }
                    }
                    if (excluded) continue;
                }

                Map<String, Object> node = new HashMap<>();
                node.put("name", name);
                node.put("path", rootDir.relativize(entry).toString());
                node.put("isDirectory", Files.isDirectory(entry));

                if (Files.isDirectory(entry)) {
                    node.put("children", buildFileTree(entry, rootDir));
                } else {
                    node.put("size", Files.size(entry));
                    node.put("extension", getExtension(name));
                }

                result.add(node);
            }
        }

        return result;
    }

    /**
     * 获取文件扩展名
     */
    private String getExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    /**
     * 判断是否是文本文件
     */
    private boolean isTextFile(String extension) {
        return extension.isEmpty() || TEXT_EXTENSIONS.contains(extension);
    }

    /**
     * 根据扩展名获取 MediaType（用于流式返回文件内容）
     *
     * @param extension 文件扩展名
     * @return 对应的 MediaType，未知类型返回 APPLICATION_OCTET_STREAM
     */
    private MediaType getMediaType(String extension) {
        return switch (extension) {
            // 图片
            case "png" -> MediaType.IMAGE_PNG;
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "gif" -> MediaType.IMAGE_GIF;
            case "svg" -> MediaType.parseMediaType("image/svg+xml");
            case "webp" -> MediaType.parseMediaType("image/webp");
            case "bmp" -> MediaType.parseMediaType("image/bmp");
            case "ico" -> MediaType.parseMediaType("image/x-icon");
            case "avif" -> MediaType.parseMediaType("image/avif");
            // 视频
            case "mp4" -> MediaType.parseMediaType("video/mp4");
            case "webm" -> MediaType.parseMediaType("video/webm");
            case "ogg", "ogv" -> MediaType.parseMediaType("video/ogg");
            case "avi" -> MediaType.parseMediaType("video/x-msvideo");
            case "mov" -> MediaType.parseMediaType("video/quicktime");
            case "mkv" -> MediaType.parseMediaType("video/x-matroska");
            // 音频
            case "mp3" -> MediaType.parseMediaType("audio/mpeg");
            case "wav" -> MediaType.parseMediaType("audio/wav");
            case "oga" -> MediaType.parseMediaType("audio/ogg");
            case "flac" -> MediaType.parseMediaType("audio/flac");
            case "aac" -> MediaType.parseMediaType("audio/aac");
            case "m4a" -> MediaType.parseMediaType("audio/mp4");
            // 字体
            case "woff" -> MediaType.parseMediaType("font/woff");
            case "woff2" -> MediaType.parseMediaType("font/woff2");
            case "ttf" -> MediaType.parseMediaType("font/ttf");
            case "otf" -> MediaType.parseMediaType("font/otf");
            // PDF
            case "pdf" -> MediaType.APPLICATION_PDF;
            // 默认
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }

    /**
     * 根据扩展名获取语言标识
     */
    private String getLanguage(String extension) {
        return switch (extension) {
            case "java" -> "java";
            case "py" -> "python";
            case "js", "jsx" -> "javascript";
            case "ts", "tsx" -> "typescript";
            case "html", "htm" -> "html";
            case "css", "scss", "less" -> "css";
            case "xml" -> "xml";
            case "json" -> "json";
            case "yaml", "yml" -> "yaml";
            case "sql" -> "sql";
            case "sh", "bash" -> "bash";
            case "md" -> "markdown";
            case "vue" -> "vue";
            case "c", "h" -> "c";
            case "cpp", "hpp" -> "cpp";
            case "cs" -> "csharp";
            case "go" -> "go";
            case "rs" -> "rust";
            case "rb" -> "ruby";
            case "php" -> "php";
            case "swift" -> "swift";
            case "kt" -> "kotlin";
            case "dart" -> "dart";
            case "lua" -> "lua";
            case "r" -> "r";
            default -> "plaintext";
        };
    }
}
