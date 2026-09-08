package com.acme.agentstudio.domain.knowledge.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * 单条 Golden 测试用例的离线评测观察采样数据实体 Record（RAG Evaluation Observation）。
 * 包含用例 ID caseId、评测模式 mode (RagEvaluationMode)、期望命中的文档 ID 列表 expectedDocumentIds、
 * 实际检索到的文档 ID 列表 retrievedDocumentIds、端到端延时 latencyMs (毫秒)、
 * 是否调用重排模型 rerankerCalled 及发生的 Reranker 扣费金额 rerankerCost。
 *
 * @param caseId 测试用例唯一 ID
 * @param mode 实验对比模式
 * @param expectedDocumentIds 标注的标准期望命中文档 ID 列表
 * @param retrievedDocumentIds 检索系统实际召回截取的文档 ID 列表
 * @param latencyMs 端到端耗时（毫秒）
 * @param rerankerCalled 是否触发了重排模型调用
 * @param rerankerCost 重排模型消耗金额
 */
public record RagEvaluationObservation(
        String caseId,
        RagEvaluationMode mode,
        List<Long> expectedDocumentIds,
        List<Long> retrievedDocumentIds,
        long latencyMs,
        boolean rerankerCalled,
        BigDecimal rerankerCost
) {
    /** 紧凑构造函数做输入校验与默认值初始化 */
    public RagEvaluationObservation {
        caseId = (caseId == null) ? "" : caseId;
        mode = (mode == null) ? RagEvaluationMode.DENSE_SPARSE_EXACT : mode;
        expectedDocumentIds = (expectedDocumentIds == null) ? List.of() : List.copyOf(expectedDocumentIds);
        retrievedDocumentIds = (retrievedDocumentIds == null) ? List.of() : List.copyOf(retrievedDocumentIds);
        latencyMs = Math.max(0L, latencyMs);
        rerankerCost = (rerankerCost == null) ? BigDecimal.ZERO : rerankerCost.max(BigDecimal.ZERO);
    }
}

