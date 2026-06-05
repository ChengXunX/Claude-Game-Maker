package com.chengxun.gamemaker.model;

import java.util.List;
import java.util.Map;

/**
 * AI助手工具定义
 * 定义AI助手可以调用的系统工具
 *
 * @author chengxun
 * @since 1.0.0
 */
public class AiTool {

    /** 工具名称 */
    private String name;

    /** 工具描述 */
    private String description;

    /** 参数定义 */
    private Map<String, ParameterDef> parameters;

    /** 所需权限 */
    private String permission;

    public AiTool() {}

    public AiTool(String name, String description, Map<String, ParameterDef> parameters, String permission) {
        this.name = name;
        this.description = description;
        this.parameters = parameters;
        this.permission = permission;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Map<String, ParameterDef> getParameters() { return parameters; }
    public void setParameters(Map<String, ParameterDef> parameters) { this.parameters = parameters; }

    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }

    /**
     * 参数定义
     */
    public static class ParameterDef {
        private String type;
        private String description;
        private boolean required;
        private List<String> enumValues;

        public ParameterDef() {}

        public ParameterDef(String type, String description, boolean required) {
            this.type = type;
            this.description = description;
            this.required = required;
        }

        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public boolean isRequired() { return required; }
        public void setRequired(boolean required) { this.required = required; }

        public List<String> getEnumValues() { return enumValues; }
        public void setEnumValues(List<String> enumValues) { this.enumValues = enumValues; }
    }
}
