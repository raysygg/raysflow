package com.acme.agentstudio.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * EmbeddingPreview 请求数据传输对象 (DTO)。
 * 封装前端或外部传入的 EmbeddingPreview 操作参数。
 */
/**
 * Embeddings 向量化效果预览请求对象。
 *
 * @param text 需要验证切片与向量提取效果的测试文本内容
 */
/**
 * EmbeddingPreview 业务请求数据传输对象 (DTO)。
 */
public record EmbeddingPreviewRequest(
        @Schema(description = "需要验证向量化效果的文本") String text
) {
}

