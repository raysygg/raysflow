package com.acme.agentstudio.common.exception;

import com.acme.agentstudio.common.response.ApiErrorCode;
import com.acme.agentstudio.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;

/**
 * 全局 RestController 异常处理切面拦截器（Global Exception Handler）。
 * 将系统内部抛出的各类业务与领域异常统一转换为结构稳定的中文 API 错误响应体，保障前后端交互协议的一致性。
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理非法请求参数异常 (HTTP 400)。
     *
     * @param exception 参数校验失败异常实例
     * @return 状态码 400 的统一错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<?>> handleBadRequest(IllegalArgumentException exception) {
        log.warn("请求参数校验失败：{}", exception.getMessage());
        return jsonError(HttpStatus.BAD_REQUEST, ApiResponse.fail(ApiErrorCode.BAD_REQUEST,
                safeMessage(exception.getMessage(), "请求参数不符合要求，请检查后重试。")));
    }

    /**
     * 处理请求体字段校验失败并返回可定位的字段路径。
     *
     * @param exception Spring 请求体校验异常
     * @return 字段级错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleFieldValidation(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        String fieldPath = fieldError == null ? null : fieldError.getField();
        String message = fieldError == null ? "提交内容校验失败，请检查表单。"
                : safeMessage(fieldError.getDefaultMessage(), "该字段填写不正确，请检查后重试。");
        return jsonError(HttpStatus.BAD_REQUEST, ApiResponse.fail(ApiErrorCode.VALIDATION_FAILED,
                message, fieldPath, false, List.of()));
    }

    /**
     * 处理查询参数和路径参数约束失败。
     *
     * @param exception 参数约束异常
     * @return 字段级错误响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleConstraintValidation(ConstraintViolationException exception) {
        ConstraintViolation<?> violation = exception.getConstraintViolations().stream().findFirst().orElse(null);
        String fieldPath = violation == null ? null : violation.getPropertyPath().toString();
        String message = violation == null ? "请求参数校验失败，请检查后重试。"
                : safeMessage(violation.getMessage(), "请求参数填写不正确，请检查后重试。");
        return jsonError(HttpStatus.BAD_REQUEST, ApiResponse.fail(ApiErrorCode.VALIDATION_FAILED,
                message, fieldPath, false, List.of()));
    }

    /**
     * 处理外部系统或第三方应用入口调用失败异常。
     *
     * @param exception 外部调用异常实例
     * @return 包含对应 HTTP 状态码的统一错误响应
     */
    @ExceptionHandler(ExternalInvocationException.class)
    public ResponseEntity<ApiResponse<?>> handleExternalInvocation(ExternalInvocationException exception) {
        log.warn("外部应用入口调用失败，code={}, message={}", exception.code(), exception.getMessage());
        return jsonError(exception.status(), ApiResponse.fail(exception.code(),
                safeMessage(exception.getMessage(), "外部能力暂时不可用，请稍后重试。"), null,
                exception.status().is5xxServerError(), List.of("请检查对应能力配置和服务状态。")));
    }

    /**
     * 处理无权访问或越权操作异常 (HTTP 403)。
     *
     * @param exception 权限被拒绝异常实例
     * @return 状态码 403 的统一错误响应
     */
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleForbidden(AuthorizationDeniedException exception) {
        log.warn("权限校验失败：{}", exception.getMessage());
        return jsonError(HttpStatus.FORBIDDEN, ApiResponse.fail(ApiErrorCode.FORBIDDEN, exception.getMessage()));
    }

    /**
     * 处理 Spring Security 权限拒绝，禁止转换为登录刷新。
     *
     * @param exception 权限拒绝异常
     * @return 403 权限错误
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDenied(AccessDeniedException exception) {
        log.warn("当前用户缺少操作权限：{}", exception.getMessage());
        return jsonError(HttpStatus.FORBIDDEN,
                ApiResponse.fail(ApiErrorCode.FORBIDDEN, "当前账号没有执行该操作的权限。"));
    }

    /**
     * 处理系统处于只读迁移状态时的操作拒绝异常 (HTTP 409)。
     *
     * @param exception 只读迁移异常实例
     * @return 状态码 409 的统一错误响应
     */
    @ExceptionHandler(MigrationReadOnlyException.class)
    public ResponseEntity<ApiResponse<?>> handleMigrationReadOnly(MigrationReadOnlyException exception) {
        log.warn("只读迁移入口被调用：{}", exception.getMessage());
        return jsonError(HttpStatus.CONFLICT,
                ApiResponse.fail(ApiErrorCode.MIGRATION_READ_ONLY, exception.getMessage()));
    }

    /**
     * 处理工作流与资源编排并发冲突异常 (HTTP 409)。
     *
     * @param exception 编排冲突异常实例
     * @return 状态码 409 的统一错误响应
     */
    @ExceptionHandler(OrchestrationConflictException.class)
    public ResponseEntity<ApiResponse<?>> handleConflict(OrchestrationConflictException exception) {
        log.warn("编排并发冲突：{}", exception.getMessage());
        return jsonError(HttpStatus.CONFLICT, ApiResponse.fail(ApiErrorCode.CONFLICT, exception.getMessage()));
    }

    /**
     * 处理模型或 Agent 运行版本不可用异常 (HTTP 503)。
     *
     * @param exception 版本不可用异常实例
     * @return 状态码 503 的统一错误响应
     */
    @ExceptionHandler(VersionUnavailableException.class)
    public ResponseEntity<ApiResponse<?>> handleVersionUnavailable(VersionUnavailableException exception) {
        log.warn("运行版本不可用：{}", exception.getMessage());
        return jsonError(HttpStatus.SERVICE_UNAVAILABLE,
                ApiResponse.fail(ApiErrorCode.VERSION_UNAVAILABLE, exception.getMessage(), null, true,
                        List.of("请检查应用活动版本和发布状态。")));
    }

    /**
     * 处理实时 WebSocket/消息通道暂不可用异常 (HTTP 503)。
     *
     * @param exception 实时链路不可用异常实例
     * @return 状态码 503 的统一错误响应
     */
    @ExceptionHandler(RealtimeUnavailableException.class)
    public ResponseEntity<ApiResponse<?>> handleRealtimeUnavailable(RealtimeUnavailableException exception) {
        log.warn("实时服务暂时不可用：{}", exception.getMessage());
        return jsonError(HttpStatus.SERVICE_UNAVAILABLE,
                ApiResponse.fail(ApiErrorCode.REALTIME_UNAVAILABLE, exception.getMessage(), null, true,
                        List.of("可稍后重试或查看运行详情。")));
    }

    /**
     * 处理 RAG 向量 Embeddings 模型服务调用异常 (HTTP 503)。
     *
     * @param exception 向量模型调用异常实例
     * @return 状态码 503 的统一错误响应
     */
    @ExceptionHandler(RagEmbeddingInvocationException.class)
    public ResponseEntity<ApiResponse<?>> handleRagEmbeddingUnavailable(
            RagEmbeddingInvocationException exception) {
        log.warn("RAG Embedding 调用失败，source={}, category={}",
                exception.modelSource(), exception.category(), exception);
        String code = ApiErrorCode.RAG_EMBEDDING_UNAVAILABLE + "_" + exception.category().name();
        return jsonError(HttpStatus.SERVICE_UNAVAILABLE, ApiResponse.fail(code,
                safeMessage(exception.getMessage(), "知识索引依赖暂时不可用，请检查模型配置后重试。"), null, true,
                List.of("请前往模型中心检查向量模型状态。")));
    }

    /**
     * 处理所有未显式捕获的未知系统异常 (HTTP 500)。
     *
     * @param exception 系统未捕获异常实例
     * @return 状态码 500 且转化为人性化提示的统一错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleUnexpected(Exception exception) {
        log.error("服务器处理请求失败", exception);
        String userFriendlyMsg = humanizeMessage(exception);
        return jsonError(HttpStatus.INTERNAL_SERVER_ERROR, ApiResponse.fail(ApiErrorCode.SERVER_ERROR, userFriendlyMsg));
    }

    /**
     * 将底层的 HTTP 超时、API Key 缺失、网络联通异常转换为面向业务的人性化操作指引。
     *
     * @param exception 捕获到的底层异常对象
     * @return 转化后的清晰中文操作指引文案
     */
    private String humanizeMessage(Exception exception) {
        String msg = exception.getMessage();
        if (msg == null) {
            return "系统处理时遇到未知错误，请稍后重试。";
        }
        if (msg.contains("ApiKey") || msg.contains("API Key") || msg.contains("credential") || msg.contains("modelId is null")) {
            return "模型 API 凭证未配置或失效，请前往【模型中心】配置对应的模型凭证。";
        }
        if (msg.contains("ConnectException") || msg.contains("timeout") || msg.contains("I/O error") || msg.contains("Connection refused")) {
            return "模型服务响应超时或网络连接失败，请检查模型 Endpoint 地址和网络连通性。";
        }
        if (msg.contains("vectorDimension") || msg.contains("dimension")) {
            return "Embedding 模型维度配置错误，请在【模型中心】检查向量维度配置。";
        }
        return "服务处理失败，请稍后重试；如持续失败，请联系管理员查看服务日志。";
    }

    /**
     * 只允许简短中文业务提示进入响应，避免底层异常和敏感内容泄漏。
     */
    private String safeMessage(String message, String fallback) {
        if (message == null || message.isBlank() || message.length() > 300) {
            return fallback;
        }
        String lower = message.toLowerCase();
        if (lower.contains("api key") || lower.contains("apikey") || lower.contains("secret")
                || lower.contains("token") || lower.contains("password") || lower.contains("jdbc:")) {
            return fallback;
        }
        boolean containsChinese = message.codePoints()
                .anyMatch(codePoint -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN);
        return containsChinese ? message : fallback;
    }

    /**
     * 辅助构造标准 JSON 响应体。
     *
     * @param status HTTP 状态码
     * @param body ApiResponse 包装数据
     * @return ResponseEntity 实例
     */
    private ResponseEntity<ApiResponse<?>> jsonError(HttpStatus status, ApiResponse<?> body) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
    }
}

