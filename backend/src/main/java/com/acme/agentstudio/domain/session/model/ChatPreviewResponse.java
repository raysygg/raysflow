package com.acme.agentstudio.domain.session.model;

import java.util.List;

/**
 * ChatPreview 响应数据传输对象 (DTO/VO)。
 * 封装返回给前端或调用方的 ChatPreview 数据体。
 */
/**
 * ChatPreview 业务响应数据展示对象 (VO/DTO)。
 */
public record ChatPreviewResponse(
        String sessionId,
        String userMessage,
        String assistantReply,
        List<String> references,
        List<String> executionSteps
) {
}
