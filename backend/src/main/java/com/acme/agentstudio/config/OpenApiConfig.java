package com.acme.agentstudio.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.0 (Swagger UI) 接口文档配置类。
 * 声明平台标准 API 的标题、版本与中文描述信息，并装配 JWT Bearer 身份认证规范（`bearerAuth`）。
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Enterprise Agent Studio API",
                version = "0.1.0",
                description = "企业级 Agent、知识库、工作流编排和运行观测接口。",
                contact = @Contact(name = "Enterprise Agent Studio Team")
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "登录接口返回的 JWT 访问令牌（AccessToken）。"
)
public class OpenApiConfig {
}

