package com.acme.agentstudio.infrastructure.model.strategy;

import com.acme.agentstudio.domain.model.ModelInvocationErrorCategory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

import java.net.ConnectException;
import java.net.HttpRetryException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.Locale;
import java.util.concurrent.TimeoutException;

/**
 * 大模型与向量模型底层调用异常统一分类器组件（Model Invocation Error Classifier）。
 * 将不同模型供应商（OpenAI, Ollama, Qwen, Zhipu, DeepSeek 等）以及底层 HTTP 客户端抛出的异构异常，
 * 归一化解析并分类映射为统一的 ModelInvocationErrorCategory 领域模型错误类别
 * （包含 AUTHENTICATION_FAILED 身份鉴权失败、RATE_LIMITED 频率超限、TIMEOUT 响应超时、DIMENSION_MISMATCH 向量维度不匹配、INVALID_RESPONSE 无法解析响应与 CONNECTION_FAILED 连接故障）。
 */
@Component
public class ModelInvocationErrorClassifier {

    /**
     * 将给定的 Throwable 异常分类归一为具体的 ModelInvocationErrorCategory 错误枚举。
     *
     * @param exception 捕获的原始异常
     * @return 归一化后的错误类别 ModelInvocationErrorCategory
     */
    public ModelInvocationErrorCategory classify(Throwable exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof RestClientResponseException responseException) {
                int status = responseException.getStatusCode().value();
                if (status == 401 || status == 403) {
                    return ModelInvocationErrorCategory.AUTHENTICATION_FAILED;
                }
                if (status == 429) {
                    return ModelInvocationErrorCategory.RATE_LIMITED;
                }
            }

            if (cause instanceof TimeoutException
                    || cause instanceof SocketTimeoutException
                    || cause instanceof HttpTimeoutException) {
                return ModelInvocationErrorCategory.TIMEOUT;
            }

            if (cause instanceof ConnectException || cause instanceof HttpRetryException) {
                return ModelInvocationErrorCategory.CONNECTION_FAILED;
            }

            cause = cause.getCause();
        }

        String normalized = safeMessage(exception).toLowerCase(Locale.ROOT);

        if (containsAny(normalized, "429", "rate limit", "too many requests", "quota")) {
            return ModelInvocationErrorCategory.RATE_LIMITED;
        }

        if (containsAny(normalized, "401", "403", "unauthorized", "forbidden", "api key", "credential")) {
            return ModelInvocationErrorCategory.AUTHENTICATION_FAILED;
        }

        if (containsAny(normalized, "timeout", "timed out")) {
            return ModelInvocationErrorCategory.TIMEOUT;
        }

        if (containsAny(normalized, "dimension", "向量维度")) {
            return ModelInvocationErrorCategory.DIMENSION_MISMATCH;
        }

        if (containsAny(normalized, "response", "parse", "json", "响应为空", "返回为空")) {
            return ModelInvocationErrorCategory.INVALID_RESPONSE;
        }

        return ModelInvocationErrorCategory.CONNECTION_FAILED;
    }

    /** 提取异常安全的错误 Message */
    private String safeMessage(Throwable exception) {
        if (exception == null || exception.getMessage() == null) {
            return "";
        }
        return exception.getMessage();
    }

    /** 判断文本是否包含包含任意一个关键词 */
    private boolean containsAny(String value, String... fragments) {
        for (String fragment : fragments) {
            if (value.contains(fragment)) {
                return true;
            }
        }
        return false;
    }
}

