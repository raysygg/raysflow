package com.acme.agentstudio.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * CreateKnowledgeTag 请求数据传输对象 (DTO)。
 * 封装前端或外部传入的 CreateKnowledgeTag 操作参数。
 */
/**
 * 创建知识库标签请求对象。
 *
 * @param tagName 标签名称（如 "产品手册", "技术规范" 等）
 * @param tagColor 标签前端展示颜色 Hex 编码（如 "#1677ff"）
 */
/**
 * CreateKnowledgeTag 业务请求数据传输对象 (DTO)。
 */
public record CreateKnowledgeTagRequest(
        @Schema(description = "标签名称", example = "产品手册") String tagName,
        @Schema(description = "标签颜色", example = "#1677ff") String tagColor
) {
}

