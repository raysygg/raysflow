package com.acme.agentstudio.infrastructure.rag;

import com.acme.agentstudio.infrastructure.persistence.entity.RagRetrievalMetricEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.RagRetrievalMetricMapper;
import com.acme.agentstudio.domain.knowledge.model.RagRetrievalOutcome;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * RagRetrievalMetric 业务服务接口。
 * 定义 RagRetrievalMetric 相关的核心业务契约与流程接口。
 */
/**
 * 记录 RAG 检索命中情况，供运营快照计算真实命中率。
 */
@Service
/**
 * RagRetrievalMetric 业务逻辑服务接口。
 * 负责 RagRetrievalMetric 核心业务逻辑与流程编排。
 */
public class RagRetrievalMetricService {

    private final RagRetrievalMetricMapper metricMapper;

    public RagRetrievalMetricService(RagRetrievalMetricMapper metricMapper) {
        this.metricMapper = metricMapper;
    }

        /**
         * record 方法。
         *
         * @param tenantId tenantId 参数
         * @param callType callType 参数
         * @param hitCount hitCount 参数
         * @param topScore topScore 参数
         * @param grounded grounded 参数
         */
    public void record(Long tenantId, String callType, int hitCount, double topScore, boolean grounded) {
        if (tenantId == null) {
            return;
        }
        RagRetrievalMetricEntity metric = new RagRetrievalMetricEntity();
        metric.setTenantId(tenantId);
        metric.setCallType(callType == null || callType.isBlank() ? "UNKNOWN" : callType);
        metric.setHitCount(Math.max(0, hitCount));
        metric.setTopScore(topScore);
        metric.setHit(hitCount > 0);
        metric.setGrounded(grounded);
        metric.setCreatedAt(LocalDateTime.now());
        metricMapper.insert(metric);
    }

    /** 记录检索摘要，避免把完整查询和文档正文写入运行指标。 */
    public void record(Long tenantId, String callType, RagRetrievalOutcome outcome, boolean grounded) {
        if (tenantId == null || outcome == null) {
            return;
        }
        RagRetrievalMetricEntity metric = new RagRetrievalMetricEntity();
        metric.setTenantId(tenantId);
        metric.setCallType(callType == null || callType.isBlank() ? "UNKNOWN" : callType);
        metric.setHitCount(outcome.hitCount());
        metric.setTopScore(outcome.topScore());
        metric.setHit(outcome.hitCount() > 0);
        metric.setGrounded(grounded);
        metric.setActualLanguage(outcome.actualLanguage().name());
        metric.setLanguageSource(outcome.languageSource().name());
        metric.setRetrievalScope(outcome.scope().name());
        metric.setRetrievalChannels(outcome.channels().stream().map(Enum::name).sorted().reduce((left, right) -> left + "," + right).orElse(""));
        metric.setCandidateCount(outcome.candidateCount());
        metric.setEmbeddingProfile(outcome.embeddingProfile());
        metric.setModelSource(outcome.modelSource());
        metric.setDegradeReason(outcome.degradeReason());
        metric.setRerankerEstimatedCost(outcome.rerankerEstimatedCost());
        metric.setRerankerBudgetExceeded(outcome.rerankerBudgetExceeded());
        if (outcome.debugSummary() != null) {
            metric.setDenseCandidateCount(outcome.debugSummary().denseCandidateCount());
            metric.setSparseCandidateCount(outcome.debugSummary().sparseCandidateCount());
            metric.setExactCandidateCount(outcome.debugSummary().exactCandidateCount());
            metric.setFusionCandidateCount(outcome.debugSummary().fusionCandidateCount());
        }
        metric.setIndexGenerationId(outcome.indexGenerationId());
        metric.setInitialCandidateCount(outcome.initialCandidateCount());
        metric.setRerankedCandidateCount(outcome.rerankedCandidateCount());
        metric.setScoreGap(outcome.scoreGap());
        metric.setQueryRewritten(outcome.queryRewritten());
        metric.setDegraded(outcome.degraded());
        metric.setParentCoverage(outcome.parentCoverage());
        metric.setEmbeddingElapsedMs(outcome.stageTimings().embeddingMs());
        metric.setVectorSearchElapsedMs(outcome.stageTimings().vectorSearchMs());
        metric.setRerankElapsedMs(outcome.stageTimings().rerankMs());
        metric.setContextElapsedMs(outcome.stageTimings().contextAssemblyMs());
        metric.setElapsedMs(outcome.elapsedMs());
        metric.setCreatedAt(LocalDateTime.now());
        metricMapper.insert(metric);
    }
}
