package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.config.SystemConstants;
import com.chengxun.gamemaker.web.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 工具结果微压缩服务
 * 对大型工具返回结果进行压缩，保留关键信息，减少 Token 消耗
 *
 * 压缩策略：
 * - read 结果：保留首尾，中间用摘要替代
 * - bash 输出：保留首尾，截断中间
 * - grep 结果：限制返回行数
 * - edit/write 结果：替换为简短确认
 *
 * 灵感来源：工具结果压缩机制
 *
 * @author chengxun
 * @since 3.0.0
 */
@Service
public class ToolResultCompactor {

    private static final Logger log = LoggerFactory.getLogger(ToolResultCompactor.class);

    /** 可压缩的工具名称 */
    private static final Pattern TOOL_RESULT_PATTERN = Pattern.compile(
        "\\[tool_result\\s+name=\"(\\w+)\"\\](.*?)\\[/tool_result\\]",
        Pattern.DOTALL
    );

    @Autowired
    private SystemConfigService configService;

    /**
     * 压缩消息中的工具结果
     * 扫描消息内容，对大型工具结果进行压缩
     *
     * @param content 原始消息内容
     * @return 压缩后的消息内容
     */
    public String compactMessage(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        if (!isEnabled()) {
            return content;
        }

        StringBuilder result = new StringBuilder();
        Matcher matcher = TOOL_RESULT_PATTERN.matcher(content);
        int lastEnd = 0;

        while (matcher.find()) {
            // 添加匹配前的文本
            result.append(content, lastEnd, matcher.start());

            String toolName = matcher.group(1);
            String toolResult = matcher.group(2);

            String compacted = compactToolResult(toolName, toolResult);
            result.append("[tool_result name=\"").append(toolName).append("\"]")
                  .append(compacted)
                  .append("[/tool_result]");

            lastEnd = matcher.end();
        }

        // 添加剩余文本
        if (lastEnd < content.length()) {
            result.append(content.substring(lastEnd));
        }

        return result.toString();
    }

    /**
     * 压缩单个工具结果
     *
     * @param toolName 工具名称
     * @param result 工具结果内容
     * @return 压缩后的内容
     */
    public String compactToolResult(String toolName, String result) {
        if (result == null || result.isEmpty()) {
            return result;
        }

        return switch (toolName) {
            case "read", "Read" -> compactReadResult(result);
            case "bash", "Bash" -> compactBashResult(result);
            case "grep", "Grep" -> compactGrepResult(result);
            case "glob", "Glob" -> compactGlobResult(result);
            case "edit", "Edit", "write", "Write" -> compactEditResult(result);
            default -> result;
        };
    }

    /**
     * 压缩文件读取结果
     * 保留首尾各 N 字符，中间用摘要替代
     */
    private String compactReadResult(String result) {
        int maxChars = getReadMaxChars();
        if (result.length() <= maxChars) {
            return result;
        }

        int headSize = maxChars / 3;
        int tailSize = maxChars / 3;
        int omitted = result.length() - headSize - tailSize;

        String head = result.substring(0, headSize);
        String tail = result.substring(result.length() - tailSize);

        return head + "\n\n... (省略 " + omitted + " 字符) ...\n\n" + tail;
    }

    /**
     * 压缩命令行输出
     * 保留首尾，截断中间
     */
    private String compactBashResult(String result) {
        int maxChars = getBashMaxChars();
        if (result.length() <= maxChars) {
            return result;
        }

        int headSize = maxChars / 3;
        int tailSize = maxChars / 3;
        int omitted = result.length() - headSize - tailSize;

        String head = result.substring(0, headSize);
        String tail = result.substring(result.length() - tailSize);

        return head + "\n\n... (省略 " + omitted + " 字符的输出) ...\n\n" + tail;
    }

    /**
     * 压缩搜索结果
     * 限制返回行数
     */
    private String compactGrepResult(String result) {
        int maxLines = getGrepMaxLines();
        String[] lines = result.split("\n");

        if (lines.length <= maxLines) {
            return result;
        }

        StringBuilder compacted = new StringBuilder();
        for (int i = 0; i < maxLines; i++) {
            compacted.append(lines[i]).append("\n");
        }
        compacted.append("\n... (共 ").append(lines.length).append(" 条结果，已显示前 ")
                 .append(maxLines).append(" 条) ...");

        return compacted.toString();
    }

    /**
     * 压缩文件列表结果
     * 限制返回数量
     */
    private String compactGlobResult(String result) {
        int maxLines = getGrepMaxLines(); // 复用 grep 的限制
        String[] lines = result.split("\n");

        if (lines.length <= maxLines) {
            return result;
        }

        StringBuilder compacted = new StringBuilder();
        for (int i = 0; i < maxLines; i++) {
            compacted.append(lines[i]).append("\n");
        }
        compacted.append("\n... (共 ").append(lines.length).append(" 个文件，已显示前 ")
                 .append(maxLines).append(" 个) ...");

        return compacted.toString();
    }

    /**
     * 压缩编辑/写入结果
     * 替换为简短确认
     */
    private String compactEditResult(String result) {
        // 提取文件路径（如果有）
        if (result.contains("Error") || result.contains("error")) {
            return result; // 错误信息不压缩
        }
        return "[操作已完成]";
    }

    /**
     * 估算压缩后的 Token 数
     *
     * @param content 原始内容
     * @return 估算的 Token 数
     */
    public long estimateTokens(String content) {
        if (content == null) return 0;
        return content.length() / 3; // 粗略估算：3字符≈1token
    }

    // ===== 配置读取 =====

    private boolean isEnabled() {
        return configService.getBoolean("compactor.enabled", true);
    }

    private int getReadMaxChars() {
        return configService.getInt("compactor.read-max-chars", 2000);
    }

    private int getBashMaxChars() {
        return configService.getInt("compactor.bash-max-chars", 1000);
    }

    private int getGrepMaxLines() {
        return configService.getInt("compactor.grep-max-lines", 20);
    }
}
