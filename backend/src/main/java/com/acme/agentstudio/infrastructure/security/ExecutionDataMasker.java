package com.acme.agentstudio.infrastructure.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

/**
 * 运行数据默认敏感信息脱敏器组件（Execution Data Masker）。
 * 针对数据库与日志展示中包含的输入（input）、Prompt、系统提示词、检索片段（rag_context）、输出（output）、密钥（apikey/secret/token/password）
 * 进行结构化与字符串的自动脱敏处理，替换为 `"*** 已脱敏 ***"`，防止高敏感业务事实泄漏至常规运营接口。
 */
@Component
public class ExecutionDataMasker {

    /** 统一脱敏替换字符串 */
    private static final String MASKED_VALUE = "*** 已脱敏 ***";

    /** 敏感 JSON 键名关键字黑名单集合（聚焦密钥、密码、令牌等保密凭据） */
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "apikey", "secret", "token", "password", "credential", "auth", "authorization", "privatekey", "accesskey"
    );

    /** Jackson JSON 序列化映射组件 */
    private final ObjectMapper objectMapper;

    /**
     * 构造函数注入 ObjectMapper。
     *
     * @param objectMapper Jackson 映射组件
     */
    public ExecutionDataMasker(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 对 JSON 字符串按敏感字段名称递归脱敏。
     *
     * @param json 原始 JSON 字符串
     * @return 脱敏后的 JSON 字符串
     */
    public String maskJson(String json) {
        return maskJson(json, null);
    }

    /**
     * 指定根字段名并对整个 JSON 进行深度结构脱敏，解析失败时降级返回脱敏占位符。
     *
     * @param json 原始 JSON 字符串
     * @param rootFieldName 根节点字段名称（可选）
     * @return 脱敏后的 JSON 字符串
     */
    public String maskJson(String json, String rootFieldName) {
        if (json == null || json.isBlank()) {
            return json;
        }
        try {
            return objectMapper.writeValueAsString(mask(objectMapper.readTree(json), rootFieldName));
        } catch (Exception exception) {
            return MASKED_VALUE;
        }
    }

    /** 递归解析 JsonNode 并替换敏感键值 */
    private JsonNode mask(JsonNode value, String fieldName) {
        if (isSensitive(fieldName)) {
            return TextNode.valueOf(MASKED_VALUE);
        }
        if (value == null || value.isNull() || value.isValueNode()) {
            return value;
        }

        if (value.isArray()) {
            ArrayNode result = objectMapper.createArrayNode();
            value.forEach(item -> result.add(mask(item, fieldName)));
            return result;
        }

        ObjectNode result = objectMapper.createObjectNode();
        value.fields().forEachRemaining(entry -> result.set(entry.getKey(), mask(entry.getValue(), entry.getKey())));
        return result;
    }

    /** 校验字段名是否落在敏感字段关键词集合中 */
    private boolean isSensitive(String fieldName) {
        return fieldName != null && SENSITIVE_KEYS.contains(fieldName.replace("-", "").toLowerCase(Locale.ROOT));
    }
}

