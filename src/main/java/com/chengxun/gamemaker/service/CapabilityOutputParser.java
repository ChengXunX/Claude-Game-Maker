package com.chengxun.gamemaker.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 能力输出解析器
 * 从 Claude 的文本输出中解析结构化的能力调用
 *
 * 三层容错解析策略：
 * 1. JSON 解析（优先）— 提取 JSON 块并解析 actions 数组
 * 2. 正则解析（JSON 失败时）— 匹配 ACTION/PARAMS 格式
 * 3. 意图提取（正则失败时）— 从自然语言中识别能力关键词
 * 4. 兜底 — 返回空调用列表，原始文本作为 response
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
public class CapabilityOutputParser {

    private static final Logger log = LoggerFactory.getLogger(CapabilityOutputParser.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 已知能力名称集合（用于意图提取和模糊匹配） */
    private volatile Set<String> knownCapabilityNames = Collections.emptySet();

    /**
     * 更新已知能力名称列表
     * 由 CapabilityRegistry 在加载能力后调用
     */
    public void updateKnownCapabilities(Set<String> capabilityNames) {
        this.knownCapabilityNames = capabilityNames != null ? capabilityNames : Collections.emptySet();
    }

    /**
     * 解析结果
     */
    public static class ParseResult {
        private final List<CapabilityCall> actions;
        private final String response;
        private final String thinking;

        public ParseResult(List<CapabilityCall> actions, String response, String thinking) {
            this.actions = actions != null ? actions : Collections.emptyList();
            this.response = response != null ? response : "";
            this.thinking = thinking;
        }

        /** 是否包含能力调用 */
        public boolean hasActions() { return !actions.isEmpty(); }

        public List<CapabilityCall> getActions() { return actions; }
        public String getResponse() { return response; }
        public String getThinking() { return thinking; }
    }

    /**
     * 解析 Claude 输出
     *
     * @param output Claude 的原始输出文本
     * @return 解析结果（actions + response）
     */
    public ParseResult parse(String output) {
        if (output == null || output.isBlank()) {
            return new ParseResult(Collections.emptyList(), "", null);
        }

        // 第一层：JSON 解析
        ParseResult jsonResult = tryParseJson(output);
        if (jsonResult != null && jsonResult.hasActions()) {
            log.debug("Parsed {} actions from JSON", jsonResult.getActions().size());
            return jsonResult;
        }

        // 第二层：正则解析
        ParseResult regexResult = tryParseRegex(output);
        if (regexResult != null && regexResult.hasActions()) {
            log.debug("Parsed {} actions from regex", regexResult.getActions().size());
            return regexResult;
        }

        // 第三层：意图提取
        ParseResult intentResult = tryParseIntent(output);
        if (intentResult != null && intentResult.hasActions()) {
            log.debug("Extracted {} actions from intent", intentResult.getActions().size());
            return intentResult;
        }

        // 兜底：纯文本响应
        return new ParseResult(Collections.emptyList(), output.trim(), null);
    }

    // ===== 第一层：JSON 解析 =====

    private ParseResult tryParseJson(String output) {
        try {
            // 提取 JSON 块（可能被 ```json 或 ``` 包裹）
            String json = extractJsonBlock(output);
            if (json == null) return null;

            // 解析为 Map
            Map<String, Object> root = objectMapper.readValue(json, new TypeReference<>() {});

            // 提取 thinking
            String thinking = (String) root.get("thinking");

            // 提取 response
            String response = (String) root.get("response");

            // 提取 actions
            List<Map<String, Object>> actionList = (List<Map<String, Object>>) root.get("actions");
            if (actionList == null || actionList.isEmpty()) {
                return new ParseResult(Collections.emptyList(), response, thinking);
            }

            List<CapabilityCall> calls = new ArrayList<>();
            for (Map<String, Object> action : actionList) {
                CapabilityCall call = parseActionMap(action);
                if (call != null) {
                    call.setParseSource(CapabilityCall.ParseSource.JSON);
                    call.setRawText(json);
                    calls.add(call);
                }
            }

            return new ParseResult(calls, response, thinking);

        } catch (Exception e) {
            log.debug("JSON parse failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从文本中提取 JSON 块
     * 支持：裸 JSON、```json ... ```、``` ... ```
     */
    private String extractJsonBlock(String text) {
        // 尝试 ```json ... ```
        Pattern jsonBlockPattern = Pattern.compile("```json\\s*\\n?(\\{[\\s\\S]*?\\})\\s*\\n?```", Pattern.MULTILINE);
        Matcher m = jsonBlockPattern.matcher(text);
        if (m.find()) return m.group(1);

        // 尝试 ``` ... ```
        Pattern codeBlockPattern = Pattern.compile("```\\s*\\n?(\\{[\\s\\S]*?\\})\\s*\\n?```", Pattern.MULTILINE);
        m = codeBlockPattern.matcher(text);
        if (m.find()) return m.group(1);

        // 尝试直接找最外层的 { ... }
        int start = text.indexOf('{');
        if (start < 0) return null;

        int depth = 0;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') depth--;
            if (depth == 0) {
                String candidate = text.substring(start, i + 1);
                // 验证是否包含 actions 关键字
                if (candidate.contains("\"actions\"") || candidate.contains("\"response\"")) {
                    return candidate;
                }
                break;
            }
        }
        return null;
    }

    /**
     * 解析 action Map 为 CapabilityCall
     */
    @SuppressWarnings("unchecked")
    private CapabilityCall parseActionMap(Map<String, Object> action) {
        String actionName = (String) action.get("action");
        if (actionName == null || actionName.isBlank()) return null;

        Map<String, Object> params = (Map<String, Object>) action.get("params");
        String reason = (String) action.get("reason");

        // 参数值类型转换
        if (params != null) {
            params = convertParamTypes(params);
        }

        return new CapabilityCall(actionName.trim(), params, reason);
    }

    // ===== 第二层：正则解析 =====

    private static final Pattern ACTION_PATTERN = Pattern.compile(
        "(?:ACTION|action|操作|能力)[：:]\\s*(\\S+)", Pattern.MULTILINE);
    private static final Pattern PARAMS_PATTERN = Pattern.compile(
        "(?:PARAMS|params|参数)[：:]\\s*(.+?)(?=\\n(?:REASON|reason|原因)|$)", Pattern.MULTILINE | Pattern.DOTALL);
    private static final Pattern REASON_PATTERN = Pattern.compile(
        "(?:REASON|reason|原因)[：:]\\s*(.+?)$", Pattern.MULTILINE);

    private ParseResult tryParseRegex(String output) {
        List<CapabilityCall> calls = new ArrayList<>();

        // 查找所有 ACTION 块
        Matcher actionMatcher = ACTION_PATTERN.matcher(output);
        while (actionMatcher.find()) {
            String actionName = actionMatcher.group(1).trim();

            // 找到 ACTION 后面的 PARAMS 和 REASON
            String afterAction = output.substring(actionMatcher.end());

            Map<String, Object> params = Collections.emptyMap();
            Matcher paramsMatcher = PARAMS_PATTERN.matcher(afterAction);
            if (paramsMatcher.find()) {
                params = parseParamsString(paramsMatcher.group(1).trim());
            }

            String reason = null;
            Matcher reasonMatcher = REASON_PATTERN.matcher(afterAction);
            if (reasonMatcher.find()) {
                reason = reasonMatcher.group(1).trim();
            }

            CapabilityCall call = new CapabilityCall(actionName, params, reason);
            call.setParseSource(CapabilityCall.ParseSource.REGEX);
            call.setRawText(output);
            calls.add(call);
        }

        if (!calls.isEmpty()) {
            // 提取 response（ACTION 之前的文本）
            int firstAction = output.indexOf("ACTION:");
            if (firstAction < 0) firstAction = output.indexOf("action:");
            if (firstAction < 0) firstAction = output.indexOf("操作:");
            String response = firstAction > 0 ? output.substring(0, firstAction).trim() : "";
            return new ParseResult(calls, response, null);
        }

        return null;
    }

    /**
     * 解析参数字符串
     * 支持格式: key=value, key=value | key=value, key: value
     */
    private Map<String, Object> parseParamsString(String paramsStr) {
        Map<String, Object> params = new LinkedHashMap<>();
        if (paramsStr == null || paramsStr.isBlank()) return params;

        // 按 | 或 , 分割
        String[] pairs = paramsStr.split("[|,，]");
        for (String pair : pairs) {
            pair = pair.trim();
            if (pair.isEmpty()) continue;

            // 按 = 或 : 分割
            int sepIndex = -1;
            for (char sep : new char[]{'=', ':'}) {
                int idx = pair.indexOf(sep);
                if (idx > 0) {
                    sepIndex = idx;
                    break;
                }
            }

            if (sepIndex > 0) {
                String key = pair.substring(0, sepIndex).trim();
                String value = pair.substring(sepIndex + 1).trim();
                params.put(key, convertValue(value));
            }
        }
        return params;
    }

    // ===== 第三层：意图提取 =====

    private ParseResult tryParseIntent(String output) {
        if (knownCapabilityNames.isEmpty()) return null;

        List<CapabilityCall> calls = new ArrayList<>();
        String lowerOutput = output.toLowerCase();

        for (String capName : knownCapabilityNames) {
            // 检查输出中是否明确包含能力名称
            if (lowerOutput.contains(capName.toLowerCase())) {
                // 提取能力名称周围的上下文作为参数
                // 这是兜底策略，参数通常不完整
                CapabilityCall call = new CapabilityCall(capName, Collections.emptyMap(),
                    "从自然语言中提取的能力调用意图");
                call.setParseSource(CapabilityCall.ParseSource.INTENT);
                call.setRawText(output);
                calls.add(call);
            }
        }

        if (!calls.isEmpty()) {
            return new ParseResult(calls, output, null);
        }
        return null;
    }

    // ===== 工具方法 =====

    /**
     * 参数值类型转换
     * 字符串 "true"/"false" → Boolean, 数字字符串 → Integer/Long
     */
    private Map<String, Object> convertParamTypes(Map<String, Object> params) {
        Map<String, Object> converted = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            converted.put(entry.getKey(), convertValue(entry.getValue()));
        }
        return converted;
    }

    private Object convertValue(Object value) {
        if (value == null) return null;
        if (value instanceof Boolean || value instanceof Number) return value;

        String str = value.toString().trim();

        // Boolean
        if ("true".equalsIgnoreCase(str)) return Boolean.TRUE;
        if ("false".equalsIgnoreCase(str)) return Boolean.FALSE;

        // Number
        try {
            if (str.contains(".")) return Double.parseDouble(str);
            long l = Long.parseLong(str);
            if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) return (int) l;
            return l;
        } catch (NumberFormatException ignored) {}

        // 去除引号
        if ((str.startsWith("\"") && str.endsWith("\"")) || (str.startsWith("'") && str.endsWith("'"))) {
            str = str.substring(1, str.length() - 1);
        }

        return str;
    }

    private Object convertValue(String value) {
        return convertValue((Object) value);
    }
}
