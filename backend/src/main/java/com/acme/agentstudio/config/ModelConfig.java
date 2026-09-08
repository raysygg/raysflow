package com.acme.agentstudio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 默认大语言模型（LLM）全局基础配置类。
 * 绑定 `app.llm` 前缀参数，配置默认的第三方 API Key、BaseUrl 与推荐调用的模型名称。
 */
@Configuration
@ConfigurationProperties(prefix = "app.llm")
public class ModelConfig {

    /** 模型 API 访问凭证 Key */
    private String apiKey;

    /** 大模型兼容接口的基准 URL 地址 */
    private String baseUrl = "https://api.openai.com/v1";

    /** 默认调用的模型编码名称 */
    private String modelName = "gpt-4o";

    /** 获取默认模型 API Key */
    public String getApiKey() {
        return apiKey;
    }

    /** 设置默认模型 API Key */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /** 获取接口基准 URL */
    public String getBaseUrl() {
        return baseUrl;
    }

    /** 设置接口基准 URL */
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /** 获取默认模型名称 */
    public String getModelName() {
        return modelName;
    }

    /** 设置默认模型名称 */
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
}

