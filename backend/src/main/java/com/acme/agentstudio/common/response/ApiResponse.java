package com.acme.agentstudio.common.response;

import com.acme.agentstudio.domain.common.ApplicationMessages;

import java.util.List;

/**
 * Api 响应数据传输对象 (DTO/VO)。
 * 封装返回给前端或调用方的 Api 数据体。
 */
/**
 * 统一 API 响应结果包装对象。
 * 用于规范所有 REST 接口的返回结构，确保前端能够一致解析 success、code、message 以及 data 数据域。
 *
 * @param <T> 响应数据体类型
 * @param success 操作是否成功标识
 * @param code 响应状态码（成功为 "OK"，失败为具体的 ApiErrorCode）
 * @param message 面向用户或前端提示的中文提示文案
 * @param data 实际业务响应载荷数据
 */
/**
 * Api 业务响应数据展示对象 (VO/DTO)。
 */
public record ApiResponse<T>(boolean success, String code, String message, T data,
                             String fieldPath, boolean retryable, List<String> details) {

    /** 成功响应的默认状态码 */
    public static final String SUCCESS_CODE = "OK";

    /**
     * 构建包含业务数据的标准成功响应。
     *
     * @param data 业务数据载荷
     * @param <T> 数据类型
     * @return 成功响应包装对象
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, SUCCESS_CODE, ApplicationMessages.SUCCESS, data,
                null, false, List.of());
    }

    /**
     * 构建包含自定义成功消息与业务数据的成功响应。
     *
     * @param message 自定义成功提示文案
     * @param data 业务数据载荷
     * @param <T> 数据类型
     * @return 成功响应包装对象
     */
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, SUCCESS_CODE, message, data, null, false, List.of());
    }

    /**
     * 构建默认失败响应（错误码默认为 BAD_REQUEST）。
     *
     * @param message 友好中文错误提示文案
     * @param <T> 数据类型
     * @return 失败响应包装对象
     */
    public static <T> ApiResponse<T> fail(String message) {
        return fail(ApiErrorCode.BAD_REQUEST, message);
    }

    /**
     * 构建包含指定错误码与提示文案的失败响应。
     *
     * @param code 错误编码
     * @param message 友好中文错误提示文案
     * @param <T> 数据类型
     * @return 失败响应包装对象
     */
    public static <T> ApiResponse<T> fail(String code, String message) {
        return fail(code, message, null, false, List.of());
    }

    /**
     * 构建带字段定位和安全恢复信息的失败响应。
     *
     * @param code 稳定错误编码
     * @param message 面向用户的中文错误提示
     * @param fieldPath 可选字段路径
     * @param retryable 当前操作是否允许安全重试
     * @param details 不包含凭证、请求正文或响应正文的安全说明
     * @param <T> 数据类型
     * @return 失败响应
     */
    public static <T> ApiResponse<T> fail(String code, String message, String fieldPath,
                                          boolean retryable, List<String> details) {
        return new ApiResponse<>(false, code, message, null, fieldPath, retryable,
                details == null ? List.of() : List.copyOf(details));
    }
}

