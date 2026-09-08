package com.acme.agentstudio.config;

import com.acme.agentstudio.common.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 将认证和授权过滤器中的失败统一写为 JSON API 错误契约。
 */
@Component
public class SecurityErrorResponseWriter {

    /** JSON 序列化器。 */
    private final ObjectMapper objectMapper;

    /**
     * 创建安全错误响应写入器。
     *
     * @param objectMapper JSON 序列化器
     */
    public SecurityErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 写入认证或授权失败响应，不包含内部异常内容。
     *
     * @param response HTTP 响应
     * @param status HTTP 状态码
     * @param code 稳定错误编码
     * @param message 中文用户提示
     * @throws IOException 响应写入失败
     */
    public void write(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail(code, message));
    }
}
