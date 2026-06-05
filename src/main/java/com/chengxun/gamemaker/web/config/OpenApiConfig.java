package com.chengxun.gamemaker.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger) 配置类
 * 配置API文档的基本信息和安全方案
 *
 * 主要功能：
 * - 配置API文档标题、描述、版本等信息
 * - 配置JWT认证方案
 * - 配置联系信息和许可证
 *
 * 访问地址：
 * - Swagger UI: http://localhost:9922/swagger-ui.html
 * - OpenAPI JSON: http://localhost:9922/v3/api-docs
 *
 * @author chengxun
 * @since 1.0.0
 */
@Configuration
public class OpenApiConfig {

    /**
     * 配置OpenAPI文档
     *
     * @return OpenAPI配置实例
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("ChengXun Game Maker API")
                .description("AI驱动的游戏开发自动化管理系统 API文档\n\n" +
                    "## 主要功能\n" +
                    "- Agent管理：创建、监控、调度AI Agent\n" +
                    "- 项目管理：游戏项目的创建和管理\n" +
                    "- 干预系统：人工干预Agent决策和方向\n" +
                    "- 用户管理：用户注册、审批、权限管理\n" +
                    "- 系统监控：性能监控、告警管理\n\n" +
                    "## 认证方式\n" +
                    "使用Session认证，需要先通过登录接口获取Session")
                .version("1.0.0")
                .contact(new Contact()
                    .name("ChengXun")
                    .email("support@chengxun.com"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0")))
            .addSecurityItem(new SecurityRequirement().addList("sessionAuth"))
            .components(new Components()
                .addSecuritySchemes("sessionAuth",
                    new SecurityScheme()
                        .name("SESSION")
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE)
                        .description("Session认证Cookie")));
    }
}
