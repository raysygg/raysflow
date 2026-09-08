package com.acme.agentstudio.infrastructure.rag;

import com.acme.agentstudio.domain.common.BusinessStatus;
import com.acme.agentstudio.domain.knowledge.model.RagEmbeddingProfile;
import com.acme.agentstudio.infrastructure.persistence.entity.ModelPriceEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.RagRetrievalMetricEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.ModelPriceMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.RagRetrievalMetricMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * 运行时 Reranker 重排模型费用预算守卫（Reranker Budget Guard）。
 * 在检索触发 Reranker 交叉注意力重排前，按当前租户当月已消费金额与预估预选 Tokens 成本（CostEstimate）比对，
 * 若超出租户配置的月度预算上限（rerankerBudget），返回放行决策 BudgetDecision.denied 自动降级切回纯向量/混合粗排，
 * 当模型单价价格缺失时标记为未知成本 unknownCost，不盲目将未知成本充当零成本放行。
 */
@Component
public class RerankerBudgetGuard {

    /** 每千 Tokens 计算基准数 */
    private static final BigDecimal TOKENS_PER_THOUSAND = BigDecimal.valueOf(1000L);

    /** 预估每 4 个字符折算 1 个 Token */
    private static final int TEXT_CHARS_PER_TOKEN = 4;

    /** 单条预选候选段落的预估文本字符数 */
    private static final int ESTIMATED_CANDIDATE_CHARS = 256;

    /** 模型价格配置 Mapper */
    private final ModelPriceMapper priceMapper;

    /** RAG 检索用量与指标 Mapper */
    private final RagRetrievalMetricMapper metricMapper;

    /**
     * 构造函数注入依赖 Mapper。
     */
    public RerankerBudgetGuard(ModelPriceMapper priceMapper, RagRetrievalMetricMapper metricMapper) {
        this.priceMapper = priceMapper;
        this.metricMapper = metricMapper;
    }

    /**
     * 校验租户当月 Reranker 消费预算，返回决策对象 BudgetDecision。
     *
     * @param tenantId 租户 ID
     * @param profile RAG 检索与模型配置实体 RagEmbeddingProfile
     * @param query 用户输入的 Query 文本
     * @param candidateCount 准备传入 Reranker 的候选段落数量
     * @return 预算检查决策结果 BudgetDecision
     */
    public BudgetDecision check(Long tenantId, RagEmbeddingProfile profile, String query, int candidateCount) {
        if (profile.rerankerBudget() <= 0.0D) {
            return BudgetDecision.allowed(null, false);
        }

        CostEstimate estimate = estimate(profile.rerankerModelKey(), query, candidateCount);

        BigDecimal used = metricMapper.selectList(new LambdaQueryWrapper<RagRetrievalMetricEntity>()
                        .eq(RagRetrievalMetricEntity::getTenantId, tenantId)
                        .ge(RagRetrievalMetricEntity::getCreatedAt, LocalDate.now().withDayOfMonth(1).atStartOfDay())
                        .isNotNull(RagRetrievalMetricEntity::getRerankerEstimatedCost))
                .stream()
                .map(RagRetrievalMetricEntity::getRerankerEstimatedCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean exceeded = (estimate == null)
                || used.add(estimate.cost()).compareTo(BigDecimal.valueOf(profile.rerankerBudget())) > 0;

        if (exceeded) {
            return BudgetDecision.denied(estimate);
        }
        return BudgetDecision.allowed(estimate, false);
    }

    /** 预估本次 Reranker 调用的 Tokens 消耗与理论金额 */
    private CostEstimate estimate(String modelKey, String query, int candidateCount) {
        if (modelKey == null || modelKey.isBlank()) {
            return null;
        }

        ModelPriceEntity price = priceMapper.selectOne(new LambdaQueryWrapper<ModelPriceEntity>()
                .eq(ModelPriceEntity::getModelKey, modelKey)
                .eq(ModelPriceEntity::getStatus, BusinessStatus.ACTIVE)
                .orderByDesc(ModelPriceEntity::getEffectiveFrom)
                .last("LIMIT 1"));

        if (price == null || price.getInputPricePer1k() == null) {
            return null;
        }

        long textLength = (query == null) ? 0 : query.length();
        long estimatedTokens = Math.max(1L, (textLength + (long) candidateCount * ESTIMATED_CANDIDATE_CHARS) / TEXT_CHARS_PER_TOKEN);

        BigDecimal cost = price.getInputPricePer1k()
                .multiply(BigDecimal.valueOf(estimatedTokens))
                .divide(TOKENS_PER_THOUSAND, 8, RoundingMode.HALF_UP);

        return new CostEstimate(estimatedTokens, cost);
    }

    /** 预估费用内部 Record */
    private record CostEstimate(long inputTokens, BigDecimal cost) {
    }

    /**
     * 预算判定结果实体 Record（Budget Decision）。
     *
     * @param allowed 是否允许执行 Reranker
     * @param estimatedCost 本次预计花费金额
     * @param estimatedInputTokens 本次预计输入的 Tokens 数
     * @param unknownCost 成本价格是否未知
     */
    public record BudgetDecision(
            boolean allowed,
            BigDecimal estimatedCost,
            long estimatedInputTokens,
            boolean unknownCost
    ) {
        /** 静态工厂：允许执行 */
        public static BudgetDecision allowed(CostEstimate estimate, boolean unknown) {
            return new BudgetDecision(
                    true,
                    (estimate == null) ? null : estimate.cost(),
                    (estimate == null) ? 0L : estimate.inputTokens(),
                    unknown || (estimate == null)
            );
        }

        /** 静态工厂：拒绝/超预算限制 */
        public static BudgetDecision denied(CostEstimate estimate) {
            return new BudgetDecision(
                    false,
                    (estimate == null) ? null : estimate.cost(),
                    (estimate == null) ? 0L : estimate.inputTokens(),
                    estimate == null
            );
        }
    }
}

