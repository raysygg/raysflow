package com.acme.agentstudio.domain.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * 结构化模型供应商枚举与其能力约束矩阵（Model Provider）。
 * 明确各厂商支持的底层能力（CHAT, EMBEDDING, RERANKER）与默认请求 BaseURL 模板，
 * 从根源上消除前端配置非支持能力（如给 Claude/Grok 配置 Embedding）的非法状态。
 */
public enum ModelProvider {

    /** OpenAI 官方及兼容协议供应商 */
    OPENAI("OpenAI", EnumSet.of(ModelCapability.CHAT, ModelCapability.EMBEDDING, ModelCapability.RERANKER), "https://api.openai.com/v1", "gpt-4o"),

    /** 阿里云 DashScope 通义千问 */
    ALIBABA_DASHSCOPE("阿里 DashScope (通义千问)", EnumSet.of(ModelCapability.CHAT, ModelCapability.EMBEDDING, ModelCapability.RERANKER), "https://dashscope.aliyuncs.com/compatible-mode/v1", "qwen-plus"),

    /** 百度千帆文心一言 */
    BAIDU_QIANFAN("百度千帆 (文心一言)", EnumSet.of(ModelCapability.CHAT, ModelCapability.EMBEDDING, ModelCapability.RERANKER), "https://qianfan.baidubce.com/v2", "ernie-4.0-8k"),

    /** 智谱 AI (GLM) */
    ZHIPU_AI("智谱 AI (GLM)", EnumSet.of(ModelCapability.CHAT, ModelCapability.EMBEDDING), "https://open.bigmodel.cn/api/paas/v4", "glm-4"),

    /** DeepSeek 深度求索 */
    DEEPSEEK("DeepSeek (深度求索)", EnumSet.of(ModelCapability.CHAT), "https://api.deepseek.com/v1", "deepseek-chat"),

    /** 字节跳动豆包 Volcengine */
    DOUBAO("字节跳动 (豆包 Volcengine)", EnumSet.of(ModelCapability.CHAT, ModelCapability.EMBEDDING), "https://ark.cn-beijing.volces.com/api/v3", "ep-2024xxxx"),

    /** 腾讯云混元 Hunyuan */
    HUNYUAN("腾讯云 (混元 Hunyuan)", EnumSet.of(ModelCapability.CHAT, ModelCapability.EMBEDDING), "https://api.hunyuan.tencentyun.com/v1", "hunyuan-pro"),

    /** 月之暗面 Kimi */
    MOONSHOT("月之暗面 (Kimi)", EnumSet.of(ModelCapability.CHAT), "https://api.moonshot.cn/v1", "moonshot-v1-8k"),

    /** Anthropic Claude */
    ANTHROPIC_CLAUDE("Anthropic (Claude)", EnumSet.of(ModelCapability.CHAT), "https://api.anthropic.com", "claude-3-5-sonnet-20241022"),

    /** Google Gemini */
    GOOGLE_GEMINI("Google (Gemini)", EnumSet.of(ModelCapability.CHAT, ModelCapability.EMBEDDING), "https://generativelanguage.googleapis.com", "gemini-1.5-pro"),

    /** xAI Grok */
    XAI_GROK("xAI (Grok)", EnumSet.of(ModelCapability.CHAT), "https://api.x.ai/v1", "grok-2-latest"),

    /** Ollama 本地私有化部署 */
    OLLAMA("Ollama (本地部署)", EnumSet.of(ModelCapability.CHAT, ModelCapability.EMBEDDING), "http://localhost:11434/v1", "llama3.1");

    /** 界面展示的友好中文名称 */
    private final String displayName;

    /** 支持的能力集合 Set<ModelCapability> */
    private final Set<ModelCapability> capabilities;

    /** 默认 Base URL 前缀 */
    private final String defaultBaseUrl;

    /** 示例模型名称 Key */
    private final String exampleModelKey;

    /** 构造函数 */
    ModelProvider(String displayName, Set<ModelCapability> capabilities, String defaultBaseUrl, String exampleModelKey) {
        this.displayName = displayName;
        this.capabilities = Collections.unmodifiableSet(capabilities);
        this.defaultBaseUrl = defaultBaseUrl;
        this.exampleModelKey = exampleModelKey;
    }

    /** 获取显示名称 */
    public String displayName() {
        return displayName;
    }

    /** 获取能力集合 */
    public Set<ModelCapability> capabilities() {
        return capabilities;
    }

    /** 获取默认 Base URL */
    public String defaultBaseUrl() {
        return defaultBaseUrl;
    }

    /** 获取示例模型 Key */
    public String exampleModelKey() {
        return exampleModelKey;
    }

    /**
     * 判断当前供应商是否支持指定的能力类型。
     *
     * @param capability 校验的能力枚举
     * @return true 表示支持
     */
    public boolean supports(ModelCapability capability) {
        return capability != null && capabilities.contains(capability);
    }

    /**
     * 兼容性安全的 Code 编码与模糊关键词解析转换方法。
     *
     * @param code 输入字符串（例如 qwen, dashscope, claude, gemini 等）
     * @return 匹配的 ModelProvider，默认兜底返回 OPENAI
     */
    public static ModelProvider fromCode(String code) {
        if (code == null || code.isBlank()) {
            return OPENAI;
        }

        String normalized = code.trim().toUpperCase();
        for (ModelProvider provider : values()) {
            if (provider.name().equals(normalized)) {
                return provider;
            }
        }

        if (normalized.contains("QWEN") || normalized.contains("DASHSCOPE") || normalized.contains("ALIYUN")) {
            return ALIBABA_DASHSCOPE;
        }
        if (normalized.contains("ERNIE") || normalized.contains("BAIDU") || normalized.contains("QIANFAN")) {
            return BAIDU_QIANFAN;
        }
        if (normalized.contains("CLAUDE") || normalized.contains("ANTHROPIC")) {
            return ANTHROPIC_CLAUDE;
        }
        if (normalized.contains("GEMINI") || normalized.contains("GOOGLE")) {
            return GOOGLE_GEMINI;
        }
        if (normalized.contains("GROK") || normalized.contains("XAI")) {
            return XAI_GROK;
        }
        return OPENAI;
    }
}

