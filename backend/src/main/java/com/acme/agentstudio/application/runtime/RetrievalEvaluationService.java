package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.model.PromptEvaluationCase;
import com.acme.agentstudio.domain.runtime.model.RetrievalEvaluationReport;
import com.acme.agentstudio.domain.runtime.model.RetrievalPolicy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 知识库 RAG 检索效果离线评测服务（Retrieval Evaluation Service）。
 * 使用固定的测试用例集（PromptEvaluationCase）对特定 RAG 检索策略（RetrievalPolicy）进行命中率（Hit Rate）
 * 与预期引用覆盖率（Citation Coverage）的离线评测，出具可量化的评测报告（RetrievalEvaluationReport）。
 */
@Service
public class RetrievalEvaluationService {

    /** 检索策略评估依赖的策略服务 */
    private final RetrievalPolicyService retrievalPolicyService;

    /**
     * 构造函数注入依赖策略服务。
     *
     * @param retrievalPolicyService 检索策略服务 RetrievalPolicyService
     */
    public RetrievalEvaluationService(RetrievalPolicyService retrievalPolicyService) {
        this.retrievalPolicyService = retrievalPolicyService;
    }

    /**
     * 针对指定测试样例列表评估 RAG 策略的命中率与引用覆盖度。
     *
     * @param tenantId 租户 ID
     * @param policyName 策略名称
     * @param policy 检索策略参数实体 RetrievalPolicy
     * @param cases 测试样例列表 List&lt;PromptEvaluationCase&gt;
     * @return 导出的检索评测报告实体 RetrievalEvaluationReport
     */
    public RetrievalEvaluationReport evaluate(
            long tenantId,
            String policyName,
            RetrievalPolicy policy,
            List<PromptEvaluationCase> cases
    ) {
        List<String> failures = new ArrayList<>();
        int hitCases = 0;
        int citationCases = 0;
        List<PromptEvaluationCase> safeCases = (cases == null) ? List.of() : cases;

        for (PromptEvaluationCase testCase : safeCases) {
            RetrievalPolicyService.RetrievalResult result = retrievalPolicyService.retrieve(tenantId, testCase.input(), null, policy);
            Set<String> sources = new HashSet<>();
            result.citations().forEach(citation -> sources.add(citation.sourceId()));

            if (!result.noHit()) {
                hitCases++;
            }
            if (!testCase.expectedSourceIds().isEmpty() && sources.stream().anyMatch(testCase.expectedSourceIds()::contains)) {
                citationCases++;
            } else if (!testCase.expectedSourceIds().isEmpty()) {
                failures.add(testCase.caseId() + ": 未能命中期望的知识库引用文档源 " + testCase.expectedSourceIds());
            }
        }

        int total = safeCases.size();
        int citationTotal = (int) safeCases.stream()
                .filter(item -> !item.expectedSourceIds().isEmpty())
                .count();

        double hitRate = (total == 0) ? 0.0D : (double) hitCases / total;
        double citationCoverage = (citationTotal == 0) ? 0.0D : (double) citationCases / citationTotal;

        return new RetrievalEvaluationReport(
                policyName,
                total,
                hitCases,
                hitRate,
                citationCoverage,
                Map.of("citationCases", citationTotal),
                failures
        );
    }
}

