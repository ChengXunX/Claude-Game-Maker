package com.chengxun.gamemaker.web.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 文件操作工具类
 * 提供文件和目录操作的通用方法
 *
 * 主要功能：
 * - 读取文件内容
 * - 列出目录内容
 * - 搜索文件内容
 * - 文件类型判断
 * - 路径安全检查
 *
 * 使用示例：
 * ```java
 * // 读取文件内容
 * String content = FileOperationUtil.readFile(filePath);
 *
 * // 列出目录内容
 * List<FileEntry> files = FileOperationUtil.listDirectory(dirPath);
 *
 * // 搜索文件内容
 * List<SearchResult> results = FileOperationUtil.searchInDirectory(dirPath, "keyword");
 * ```
 *
 * @author chengxun
 * @since 1.0.0
 */
public final class FileOperationUtil {

    private static final Logger log = LoggerFactory.getLogger(FileOperationUtil.class);

    /** 文本文件扩展名列表 */
    private static final String[] TEXT_FILE_EXTENSIONS = {
        ".java", ".py", ".js", ".ts", ".jsx", ".tsx", ".html", ".css", ".json",
        ".xml", ".yml", ".yaml", ".md", ".txt", ".sh", ".bat", ".sql", ".properties",
        ".cfg", ".conf", ".ini", ".toml", ".gradle", ".pom", ".vue", ".svelte",
        ".c", ".cpp", ".h", ".go", ".rs", ".rb", ".php", ".swift", ".kt"
    };

    private FileOperationUtil() {
        // 工具类不允许实例化
    }

    /**
     * 读取文件内容
     *
     * @param filePath 文件路径
     * @return 文件内容
     * @throws IOException 当文件读取失败时抛出
     */
    public static String readFile(Path filePath) throws IOException {
        if (!Files.exists(filePath)) {
            throw new IOException("文件不存在: " + filePath);
        }
        if (!Files.isRegularFile(filePath)) {
            throw new IOException("不是文件: " + filePath);
        }
        return Files.readString(filePath);
    }

    /**
     * 安全读取文件内容（不抛出异常）
     *
     * @param filePath 文件路径
     * @return 文件内容，如果读取失败返回null
     */
    public static String readFileSafe(Path filePath) {
        try {
            return readFile(filePath);
        } catch (IOException e) {
            log.warn("Failed to read file: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取文件大小
     *
     * @param filePath 文件路径
     * @return 文件大小（字节）
     * @throws IOException 当获取失败时抛出
     */
    public static long getFileSize(Path filePath) throws IOException {
        return Files.size(filePath);
    }

    /**
     * 列出目录内容
     *
     * @param dirPath 目录路径
     * @return 文件条目列表
     */
    public static List<FileEntry> listDirectory(Path dirPath) {
        List<FileEntry> entries = new ArrayList<>();

        try (Stream<Path> stream = Files.list(dirPath)) {
            stream.sorted((a, b) -> {
                    boolean aDir = Files.isDirectory(a);
                    boolean bDir = Files.isDirectory(b);
                    if (aDir != bDir) return aDir ? -1 : 1;
                    return a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString());
                })
                .forEach(path -> {
                    try {
                        boolean isDir = Files.isDirectory(path);
                        long size = isDir ? 0 : Files.size(path);
                        Integer children = isDir ? countChildren(path) : null;
                        entries.add(new FileEntry(
                            path.getFileName().toString(),
                            isDir,
                            size,
                            children
                        ));
                    } catch (IOException e) {
                        entries.add(new FileEntry(
                            path.getFileName().toString(),
                            Files.isDirectory(path),
                            0,
                            null
                        ));
                    }
                });
        } catch (IOException e) {
            log.warn("Failed to list directory: {}", e.getMessage());
        }

        return entries;
    }

    /**
     * 统计目录子项数量
     *
     * @param dirPath 目录路径
     * @return 子项数量
     */
    public static int countChildren(Path dirPath) {
        try (Stream<Path> stream = Files.list(dirPath)) {
            return (int) stream.count();
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * 在目录中搜索文件内容
     *
     * @param dirPath 目录路径
     * @param keyword 搜索关键词
     * @param maxResults 最大结果数
     * @return 搜索结果列表
     */
    public static List<SearchResult> searchInDirectory(Path dirPath, String keyword, int maxResults) {
        List<SearchResult> results = new ArrayList<>();
        String lowerKeyword = keyword.toLowerCase();

        try (Stream<Path> stream = Files.walk(dirPath, 5)) {
            stream.filter(path -> Files.isRegularFile(path))
                .filter(path -> !path.toString().contains("/.git/"))
                .filter(path -> isTextFile(path.getFileName().toString()))
                .forEach(path -> {
                    try {
                        String content = Files.readString(path);
                        String[] lines = content.split("\n");
                        for (int i = 0; i < lines.length; i++) {
                            if (lines[i].toLowerCase().contains(lowerKeyword)) {
                                String relPath = dirPath.relativize(path).toString();
                                String context = lines[i].trim();
                                if (context.length() > 120) {
                                    context = context.substring(0, 120) + "...";
                                }
                                results.add(new SearchResult(relPath, i + 1, context));
                                if (results.size() >= maxResults) return;
                            }
                        }
                    } catch (IOException e) {
                        // 跳过无法读取的文件
                    }
                });
        } catch (IOException e) {
            log.warn("Failed to search in directory: {}", e.getMessage());
        }

        return results;
    }

    /**
     * 在目录中搜索文件内容（默认最多200条结果）
     *
     * @param dirPath 目录路径
     * @param keyword 搜索关键词
     * @return 搜索结果列表
     */
    public static List<SearchResult> searchInDirectory(Path dirPath, String keyword) {
        return searchInDirectory(dirPath, keyword, 200);
    }

    /**
     * 判断是否为文本文件
     *
     * @param fileName 文件名
     * @return 是否为文本文件
     */
    public static boolean isTextFile(String fileName) {
        String lower = fileName.toLowerCase();
        for (String ext : TEXT_FILE_EXTENSIONS) {
            if (lower.endsWith(ext)) return true;
        }
        return !lower.contains(".");
    }

    /**
     * 检查路径是否在指定目录内（防止路径遍历攻击）
     *
     * @param basePath 基础目录
     * @param targetPath 目标路径
     * @return 是否在基础目录内
     */
    public static boolean isPathUnderDirectory(Path basePath, Path targetPath) {
        try {
            Path normalizedBase = basePath.normalize().toAbsolutePath();
            Path normalizedTarget = targetPath.normalize().toAbsolutePath();
            return normalizedTarget.startsWith(normalizedBase);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 安全地规范化路径（防止路径遍历攻击）
     *
     * @param basePath 基础目录
     * @param relativePath 相对路径
     * @return 规范化后的路径，如果路径不安全返回null
     */
    public static Path safeResolve(Path basePath, String relativePath) {
        Path resolved = basePath.resolve(relativePath).normalize();
        if (isPathUnderDirectory(basePath, resolved)) {
            return resolved;
        }
        return null;
    }

    /**
     * 获取文件扩展名
     *
     * @param fileName 文件名
     * @return 文件扩展名（包含点号），如果没有扩展名返回空字符串
     */
    public static String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDotIndex);
    }

    /**
     * 获取不带扩展名的文件名
     *
     * @param fileName 文件名
     * @return 不带扩展名的文件名
     */
    public static String getFileNameWithoutExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return fileName;
        }
        return fileName.substring(0, lastDotIndex);
    }

    /**
     * 文件条目记录类
     */
    public record FileEntry(String name, boolean directory, long size, Integer children) {
    }

    /**
     * 搜索结果记录类
     */
    public record SearchResult(String file, int line, String context) {
    }
}
