package com.acme.agentstudio.common.response;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 统一 API 错误响应契约测试。
 */
class ApiResponseTest {

    /**
     * 验证字段路径、重试标记和安全详情能够稳定传递。
     */
    @Test
    void shouldExposeStructuredFieldError() {
        ApiResponse<Void> response = ApiResponse.fail(ApiErrorCode.VALIDATION_FAILED,
                "应用名称不能为空。", "name", false, List.of("请填写应用名称。"));

        assertFalse(response.success());
        assertEquals(ApiErrorCode.VALIDATION_FAILED, response.code());
        assertEquals("name", response.fieldPath());
        assertFalse(response.retryable());
        assertEquals(List.of("请填写应用名称。"), response.details());
        assertNull(response.data());
    }
}
