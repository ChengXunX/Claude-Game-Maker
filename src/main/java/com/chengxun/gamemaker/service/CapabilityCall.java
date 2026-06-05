package com.chengxun.gamemaker.service;

import java.util.Collections;
import java.util.Map;

/**
 * 能力调用请求
 * 从 Claude 输出中解析出的结构化能力调用
 *
 * @author chengxun
 * @since 2.0.0
 */
public class CapabilityCall {

    /** 能力名称（对应 AgentCapability.capabilityName） */
    private String capabilityName;

    /** 调用参数 */
    private Map<String, Object> params;

    /** 调用原因（AI 输出的 reason） */
    private String reason;

    /** 解析来源：json, regex, intent */
    private ParseSource parseSource;

    /** 原始文本（用于调试） */
    private String rawText;

    public enum ParseSource {
        JSON,   // 从 JSON 解析
        REGEX,  // 从正则匹配
        INTENT  // 从自然语言意图提取
    }

    public CapabilityCall() {}

    public CapabilityCall(String capabilityName, Map<String, Object> params, String reason) {
        this.capabilityName = capabilityName;
        this.params = params != null ? params : Collections.emptyMap();
        this.reason = reason;
    }

    public String getCapabilityName() { return capabilityName; }
    public void setCapabilityName(String capabilityName) { this.capabilityName = capabilityName; }

    public Map<String, Object> getParams() { return params; }
    public void setParams(Map<String, Object> params) { this.params = params != null ? params : Collections.emptyMap(); }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public ParseSource getParseSource() { return parseSource; }
    public void setParseSource(ParseSource parseSource) { this.parseSource = parseSource; }

    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }

    @Override
    public String toString() {
        return String.format("CapabilityCall[%s] params=%s, source=%s",
            capabilityName, params, parseSource);
    }
}
