package com.acme.agentstudio.domain.knowledge.model;

import java.util.List;

/**
 * RagEvaluation 请求数据传输对象 (DTO)。
 * 封装前端或外部传入的 RagEvaluation 操作参数。
 */
/** 离线评测请求，只接收脱敏观察值，不携带查询正文或文档正文。 */
public record RagEvaluationRequest(List<RagEvaluationObservation> observations, int k) {
    public RagEvaluationRequest {
        observations = observations == null ? List.of() : List.copyOf(observations);
    }
}
