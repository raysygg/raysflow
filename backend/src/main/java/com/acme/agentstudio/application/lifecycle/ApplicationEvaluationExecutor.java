package com.acme.agentstudio.application.lifecycle;

import com.acme.agentstudio.application.runtime.RuntimeRunApplicationService;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.lifecycle.ApplicationLifecycleContracts.EvaluationMetricState;
import com.acme.agentstudio.domain.lifecycle.ApplicationLifecycleContracts.EvaluationStatus;
import com.acme.agentstudio.infrastructure.persistence.entity.ApplicationEvaluationCaseEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.ApplicationEvaluationResultEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.ApplicationEvaluationRunEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.ApplicationReleaseCandidateEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.OrchestrationVersionEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.ApplicationEvaluationCaseMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.ApplicationEvaluationResultMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.ApplicationEvaluationRunMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.OrchestrationVersionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI 应用发布候选版本（Release Candidate）自动评测与基线对比执行器。
 * 负责从 Candidate 快照及对比基线发布版本（Baseline Release）批量提取测试用例，在沙箱环境中并发/顺序运行测试套件、打分计算正确率、延迟耗时、退化样例（Regressed Cases）并落盘证据。
 */
@Service
public class ApplicationEvaluationExecutor {

    /** 评测结果：成功 */
    private static final String RESULT_SUCCESS = "SUCCEEDED";

    /** 评测结果：失败 */
    private static final String RESULT_FAILED = "FAILED";

    /** 变体标识：待评估 Candidate 候选版本 */
    private static final String VARIANT_CANDIDATE = "CANDIDATE";

    /** 变体标识：对比 Baseline 基线版本 */
    private static final String VARIANT_BASELINE = "BASELINE";

    /** 失败类型：工作流或 Agent 执行异常 */
    private static final String FAILURE_EXECUTION = "EXECUTION_FAILED";

    /** 失败类型：预期规则判定失败 */
    private static final String FAILURE_EXPECTATION = "EXPECTATION_INVALID";

    /** 评测 Run Persistence Mapper */
    private final ApplicationEvaluationRunMapper runMapper;

    /** 评测测试用例 Persistence Mapper */
    private final ApplicationEvaluationCaseMapper caseMapper;

    /** 评测结果 Persistence Mapper */
    private final ApplicationEvaluationResultMapper resultMapper;

    /** 候选版本发布服务 */
    private final ReleaseCandidateService candidateService;

    /** 实时运行沙箱引擎应用服务 */
    private final RuntimeRunApplicationService runtimeService;

    /** JSON 序列化映射工具 */
    private final ObjectMapper objectMapper;

    /** 工作流编排版本 Mapper */
    private final OrchestrationVersionMapper versionMapper;

    /**
     * 构造函数注入评测执行所需 Persistence Mapper 与运行服务。
     */
    public ApplicationEvaluationExecutor(ApplicationEvaluationRunMapper runMapper,
                                         ApplicationEvaluationCaseMapper caseMapper,
                                         ApplicationEvaluationResultMapper resultMapper,
                                         ReleaseCandidateService candidateService,
                                         RuntimeRunApplicationService runtimeService,
                                         ObjectMapper objectMapper,
                                         OrchestrationVersionMapper versionMapper) {
        this.runMapper = runMapper;
        this.caseMapper = caseMapper;
        this.resultMapper = resultMapper;
        this.candidateService = candidateService;
        this.runtimeService = runtimeService;
        this.objectMapper = objectMapper;
        this.versionMapper = versionMapper;
    }

    /**
     * 执行指定 Run ID 的完整测试套件评测任务。
     *
     * @param payload 评测任务异步 Payload 参数（包含 runId, candidateId, suiteVersionId 等）
     * @param tenantId 租户 ID
     */
    @Transactional
    public void execute(ApplicationEvaluationService.ApplicationEvaluationTaskPayload payload, Long tenantId) {
        ApplicationEvaluationRunEntity run = runMapper.selectOne(new LambdaQueryWrapper<ApplicationEvaluationRunEntity>()
                .eq(ApplicationEvaluationRunEntity::getTenantId, tenantId)
                .eq(ApplicationEvaluationRunEntity::getId, payload.runId())
                .eq(ApplicationEvaluationRunEntity::getApplicationId, payload.applicationId()));
        if (run == null || EvaluationStatus.SUCCEEDED.name().equals(run.getEvaluationStatus())) {
            return;
        }

        runMapper.update(null, new LambdaUpdateWrapper<ApplicationEvaluationRunEntity>()
                .eq(ApplicationEvaluationRunEntity::getTenantId, tenantId)
                .eq(ApplicationEvaluationRunEntity::getId, run.getId())
                .set(ApplicationEvaluationRunEntity::getEvaluationStatus, EvaluationStatus.RUNNING.name())
                .set(ApplicationEvaluationRunEntity::getStartedAt, LocalDateTime.now())
                .set(ApplicationEvaluationRunEntity::getUpdatedAt, LocalDateTime.now()));

        SecurityUser user = new SecurityUser(payload.userId(), tenantId, "evaluation-worker", "SYSTEM");
        ApplicationReleaseCandidateEntity candidate = candidateService.requireCandidate(user, payload.applicationId(), payload.candidateId());
        List<ApplicationEvaluationCaseEntity> cases = caseMapper.selectList(new LambdaQueryWrapper<ApplicationEvaluationCaseEntity>()
                .eq(ApplicationEvaluationCaseEntity::getTenantId, tenantId)
                .eq(ApplicationEvaluationCaseEntity::getApplicationId, payload.applicationId())
                .eq(ApplicationEvaluationCaseEntity::getSuiteVersionId, payload.suiteVersionId())
                .orderByAsc(ApplicationEvaluationCaseEntity::getSortOrder));

        JsonNode candidateSnapshot = readJson(candidate.getSnapshotJson());
        String candidateGraph = write(candidateSnapshot.path("graph"));
        int succeeded = 0;
        for (ApplicationEvaluationCaseEntity evaluationCase : cases) {
            ApplicationEvaluationResultEntity result = evaluateCase(user, candidateGraph, VARIANT_CANDIDATE, run, evaluationCase);
            resultMapper.insert(result);
            if (RESULT_SUCCESS.equals(result.getResultStatus())) {
                succeeded++;
            }
        }

        if (run.getBaselineReleaseId() != null && !run.getBaselineReleaseId().isBlank()) {
            OrchestrationVersionEntity baseline = versionMapper.selectOne(new LambdaQueryWrapper<OrchestrationVersionEntity>()
                    .eq(OrchestrationVersionEntity::getTenantId, tenantId)
                    .eq(OrchestrationVersionEntity::getAppId, payload.applicationId())
                    .eq(OrchestrationVersionEntity::getVersionId, run.getBaselineReleaseId())
                    .eq(OrchestrationVersionEntity::getStatus, "PUBLISHED"));
            if (baseline == null) {
                throw new IllegalStateException("基线 Release 版本不存在或状态非已发布 PUBLISHED。");
            }
            for (ApplicationEvaluationCaseEntity evaluationCase : cases) {
                resultMapper.insert(evaluateCase(user, baseline.getGraphJson(), VARIANT_BASELINE, run, evaluationCase));
            }
        }

        String status = succeeded == cases.size() ? EvaluationStatus.SUCCEEDED.name()
                : succeeded == 0 ? EvaluationStatus.FAILED.name() : EvaluationStatus.PARTIAL.name();

        List<ApplicationEvaluationResultEntity> savedResults = resultMapper.selectList(
                new LambdaQueryWrapper<ApplicationEvaluationResultEntity>()
                        .eq(ApplicationEvaluationResultEntity::getTenantId, tenantId)
                        .eq(ApplicationEvaluationResultEntity::getApplicationId, payload.applicationId())
                        .eq(ApplicationEvaluationResultEntity::getEvaluationRunId, run.getId()));

        double averageLatency = savedResults.stream()
                .map(ApplicationEvaluationResultEntity::getLatencyMs)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .average()
                .orElse(0D);

        int baselineSucceeded = (int) savedResults.stream()
                .filter(item -> VARIANT_BASELINE.equals(item.getEvaluationVariant()))
                .filter(item -> RESULT_SUCCESS.equals(item.getResultStatus()))
                .count();

        Set<Long> candidatePassed = savedResults.stream()
                .filter(item -> VARIANT_CANDIDATE.equals(item.getEvaluationVariant()))
                .filter(item -> RESULT_SUCCESS.equals(item.getResultStatus()))
                .map(ApplicationEvaluationResultEntity::getCaseId)
                .collect(Collectors.toSet());

        List<Long> regressedCases = savedResults.stream()
                .filter(item -> VARIANT_BASELINE.equals(item.getEvaluationVariant()))
                .filter(item -> RESULT_SUCCESS.equals(item.getResultStatus()))
                .map(ApplicationEvaluationResultEntity::getCaseId)
                .filter(caseId -> !candidatePassed.contains(caseId))
                .toList();

        EvaluationAggregateReport report = new EvaluationAggregateReport(
                cases.size(),
                succeeded,
                cases.isEmpty() ? 0D : (double) succeeded / cases.size(),
                averageLatency,
                savedResults.stream()
                        .map(ApplicationEvaluationResultEntity::getEstimatedCost)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                baselineSucceeded,
                cases.isEmpty() ? null : (double) baselineSucceeded / cases.size(),
                cases.isEmpty() ? null : (double) (succeeded - baselineSucceeded) / cases.size(),
                regressedCases
        );

        runMapper.update(null, new LambdaUpdateWrapper<ApplicationEvaluationRunEntity>()
                .eq(ApplicationEvaluationRunEntity::getTenantId, tenantId)
                .eq(ApplicationEvaluationRunEntity::getId, run.getId())
                .set(ApplicationEvaluationRunEntity::getEvaluationStatus, status)
                .set(ApplicationEvaluationRunEntity::getAggregateReportJson, write(report))
                .set(ApplicationEvaluationRunEntity::getCompletedAt, LocalDateTime.now())
                .set(ApplicationEvaluationRunEntity::getUpdatedAt, LocalDateTime.now()));
    }

    /**
     * 评测单个测试用例（Case）在特定图定义变体（Candidate 或 Baseline）下的执行表现与预期匹配打分。
     */
    @SuppressWarnings("unchecked")
    private ApplicationEvaluationResultEntity evaluateCase(SecurityUser user, String graphJson, String variant,
                                                            ApplicationEvaluationRunEntity run,
                                                            ApplicationEvaluationCaseEntity evaluationCase) {
        ApplicationEvaluationResultEntity result = new ApplicationEvaluationResultEntity();
        result.setTenantId(run.getTenantId());
        result.setApplicationId(run.getApplicationId());
        result.setEvaluationRunId(run.getId());
        result.setCaseId(evaluationCase.getId());
        result.setEvaluationVariant(variant);
        result.setCreatedAt(LocalDateTime.now());
        // 测试用例输入预先确定，无论执行成功还是失败均初始化输入 Token 与摘要，保证必填字段满足持久化约束
        String inputJson = evaluationCase.getInputJson() == null ? "{}" : evaluationCase.getInputJson();
        String inputContent = extractInputText(inputJson);
        result.setInputTokens(estimateTokens(inputJson));
        result.setInputDigest(digest(inputJson));
        long started = System.nanoTime();
        String expected = "";
        String actual = "";
        try {
            Map<String, Object> input = objectMapper.convertValue(objectMapper.readTree(inputJson), Map.class);
            Map<String, Object> execution = runtimeService.executeCandidateSnapshot(
                    user,
                    run.getApplicationId(),
                    "evaluation-" + run.getId() + "-" + evaluationCase.getId(),
                    graphJson,
                    input
            );
            actual = extractActualOutput(execution.getOrDefault("output", ""));
            JsonNode expectedRule = objectMapper.readTree(evaluationCase.getExpectedRuleJson());
            expected = expectedRule.path("expected").asText(expectedRule.isTextual() ? expectedRule.asText() : "");
            if (expected.isBlank()) {
                throw new IllegalArgumentException(FAILURE_EXPECTATION);
            }

            SimilarityResult similarity = evaluateSimilarity(actual, expected, expectedRule);
            boolean matched = similarity.passed();

            JsonNode applicability = readApplicability(evaluationCase.getMetricApplicabilityJson());
            result.setResultStatus(matched ? RESULT_SUCCESS : RESULT_FAILED);
            result.setCorrectnessScore(BigDecimal.valueOf(similarity.score()).setScale(4, java.math.RoundingMode.HALF_UP));
            result.setTaskSuccessScore(matched ? BigDecimal.ONE : BigDecimal.ZERO);
            result.setGroundednessScore(applicable(applicability, "groundedness")
                    ? (matched ? BigDecimal.ONE : BigDecimal.ZERO) : null);
            result.setCitationScore(applicable(applicability, "citation")
                    ? (matched ? BigDecimal.ONE : BigDecimal.ZERO) : null);
            result.setOutputTokens(estimateTokens(actual));
            result.setOutputDigest(digest(actual));

            if (!matched) {
                result.setFailureCategory("SIMILARITY_BELOW_THRESHOLD");
                result.setFailureMessage(similarity.message());
            }

            Map<String, Object> evidenceData = new java.util.LinkedHashMap<>();
            evidenceData.put("metricStates", new EvaluationEvidence(
                    EvaluationMetricState.SCORED,
                    applicable(applicability, "groundedness") ? EvaluationMetricState.SCORED : EvaluationMetricState.NOT_APPLICABLE,
                    applicable(applicability, "citation") ? EvaluationMetricState.SCORED : EvaluationMetricState.NOT_APPLICABLE,
                    EvaluationMetricState.UNAVAILABLE));
            evidenceData.put("input", inputContent);
            evidenceData.put("expected", expected);
            evidenceData.put("actualOutput", actual);
            evidenceData.put("matched", matched);
            evidenceData.put("similarityScore", similarity.score());
            evidenceData.put("similarityThreshold", similarity.threshold());
            evidenceData.put("bestSegment", similarity.bestSegment());
            evidenceData.put("localScore", similarity.localScore());
            evidenceData.put("coverageScore", similarity.coverageScore());
            evidenceData.put("globalScore", similarity.globalScore());
            evidenceData.put("matchDetails", similarity.message());
            result.setEvidenceJson(write(evidenceData));
        } catch (Exception exception) {
            // 异常兜底：确保即使工作流引擎或网络出现异常，评测失败记录也能完整持久化
            result.setResultStatus(RESULT_FAILED);
            result.setCorrectnessScore(BigDecimal.ZERO);
            result.setTaskSuccessScore(BigDecimal.ZERO);
            result.setOutputTokens(estimateTokens(actual));
            result.setOutputDigest(digest(actual));

            String failureCat = exception.getMessage() != null && exception.getMessage().contains(FAILURE_EXPECTATION)
                    ? FAILURE_EXPECTATION : FAILURE_EXECUTION;
            String failureMsg = exception.getMessage() == null ? "执行异常" : exception.getMessage();

            result.setFailureCategory(failureCat);
            result.setFailureMessage(failureMsg);

            Map<String, Object> evidenceData = new java.util.LinkedHashMap<>();
            evidenceData.put("metricStates", new EvaluationEvidence(
                    EvaluationMetricState.UNAVAILABLE,
                    EvaluationMetricState.UNAVAILABLE,
                    EvaluationMetricState.UNAVAILABLE,
                    EvaluationMetricState.UNAVAILABLE));
            evidenceData.put("input", inputContent);
            evidenceData.put("expected", expected);
            evidenceData.put("actualOutput", actual);
            evidenceData.put("matched", false);
            evidenceData.put("matchDetails", failureMsg);
            result.setEvidenceJson(write(evidenceData));
        }
        result.setLatencyMs((System.nanoTime() - started) / 1_000_000L);
        return result;
    }

    /**
     * 从工作流运行输出中提取纯净的大模型回答文本。
     * 兼容顶层 output、result、以及 nodes.llm-*.output 等多种工作流输出结构。
     */
    private String extractActualOutput(Object rawOutput) {
        if (rawOutput == null) {
            return "";
        }
        String text = String.valueOf(rawOutput).trim();
        if (text.isEmpty()) {
            return "";
        }
        try {
            JsonNode node = objectMapper.readTree(text);
            if (node.isObject()) {
                // 1. 优先提取扁平化键名（如 "nodes.llm-1.output"、"nodes.llm.output" 等大模型节点直接输出）
                var fields = node.fields();
                String fallbackLlm = null;
                while (fields.hasNext()) {
                    var entry = fields.next();
                    String key = entry.getKey().toLowerCase(java.util.Locale.ROOT);
                    JsonNode val = entry.getValue();
                    if (key.contains("llm") && (key.contains("output") || key.contains("result") || key.contains("text"))) {
                        if (val.isTextual() && !val.asText().isBlank()) {
                            return val.asText();
                        } else if (val.isObject() && val.hasNonNull("output") && val.get("output").isTextual()) {
                            return val.get("output").asText();
                        } else if (val.isObject() && val.hasNonNull("text") && val.get("text").isTextual()) {
                            return val.get("text").asText();
                        }
                    }
                    if (key.contains("llm") && val.isTextual() && !val.asText().isBlank()) {
                        fallbackLlm = val.asText();
                    }
                }
                if (fallbackLlm != null) {
                    return fallbackLlm;
                }

                // 2. 检查嵌套的 nodes 对象结构
                JsonNode nodes = node.path("nodes");
                if (nodes.isObject()) {
                    var nodeFields = nodes.fields();
                    while (nodeFields.hasNext()) {
                        var entry = nodeFields.next();
                        String k = entry.getKey().toLowerCase(java.util.Locale.ROOT);
                        if (k.contains("llm")) {
                            JsonNode val = entry.getValue();
                            if (val.hasNonNull("output") && val.get("output").isTextual()) {
                                return val.get("output").asText();
                            }
                            if (val.hasNonNull("text") && val.get("text").isTextual()) {
                                return val.get("text").asText();
                            }
                        }
                    }
                }

                // 3. 常见业务顶层回答字段
                for (String answerKey : java.util.List.of("result", "answer", "reply", "output_text", "response")) {
                    if (node.hasNonNull(answerKey) && node.get(answerKey).isTextual()) {
                        return node.get(answerKey).asText();
                    }
                }

                // 4. 尝试提取顶层 output 字段（若 output 自身也是 JSON 则递归解析）
                if (node.hasNonNull("output") && node.get("output").isTextual()) {
                    String out = node.get("output").asText();
                    if (out.trim().startsWith("{") && out.trim().endsWith("}")) {
                        String inner = extractActualOutput(out);
                        if (!inner.isBlank() && !inner.equals(out)) {
                            return inner;
                        }
                    }
                    return out;
                }
            }
        } catch (Exception ignored) {
            // 非 JSON 格式，直接使用原始文本
        }
        return text;
    }

    /**
     * 从评测用例的输入 JSON 中提取供展示与比对的业务文本。
     */
    private String extractInputText(String inputJson) {
        if (inputJson == null || inputJson.isBlank()) {
            return "";
        }
        try {
            JsonNode node = objectMapper.readTree(inputJson);
            if (node.isObject()) {
                if (node.hasNonNull("input")) {
                    return node.get("input").asText();
                }
                if (node.hasNonNull("prompt")) {
                    return node.get("prompt").asText();
                }
                if (node.hasNonNull("query")) {
                    return node.get("query").asText();
                }
                var fields = node.fields();
                if (fields.hasNext()) {
                    return fields.next().getValue().asText();
                }
            }
        } catch (Exception ignored) {
        }
        return inputJson;
    }

    private static final java.util.Set<String> STOP_WORDS = java.util.Set.of(
            "的", "了", "在", "是", "有", "和", "与", "就", "及", "或", "也", "对",
            "根据", "按照", "一个", "我们", "你们", "他们", "进行", "可以", "需要", "通过",
            "the", "a", "an", "is", "are", "and", "or", "in", "on", "to", "for"
    );

    /**
     * 针对期望内容与大模型实际回答执行多维语义相似度综合评估。
     *
     * @param actual 大模型实际回答
     * @param expected 期望内容
     * @param expectedRule 期望规则配置
     * @return 相似度分析诊断结果
     */
    private SimilarityResult evaluateSimilarity(String actual, String expected, JsonNode expectedRule) {
        if (expected == null || expected.isBlank()) {
            return new SimilarityResult(false, 0.0, 0.50, "期望内容为空，无法进行相似度分析。", "", 0.0, 0.0, 0.0);
        }
        if (actual == null || actual.isBlank()) {
            return new SimilarityResult(false, 0.0, 0.50, "大模型实际回答为空，相似度为 0.0%。", "", 0.0, 0.0, 0.0);
        }

        double threshold = expectedRule.path("threshold").asDouble(0.50);
        if (threshold <= 0 || threshold > 1.0) {
            threshold = 0.50; // 默认相似度达标阈值 50%
        }

        // 支持同义候选词（若包含 | 或 /，计算最高匹配项）
        String[] candidates = expected.split("[|/\\n;；]+");
        SimilarityResult bestCandidateResult = null;

        for (String c : candidates) {
            String candidateText = c.trim();
            if (candidateText.isEmpty()) {
                continue;
            }
            SimilarityResult currentResult = evaluateSingleSimilarity(actual, candidateText, threshold);
            if (bestCandidateResult == null || currentResult.score() > bestCandidateResult.score()) {
                bestCandidateResult = currentResult;
            }
        }

        return bestCandidateResult != null ? bestCandidateResult
                : evaluateSingleSimilarity(actual, expected, threshold);
    }

    /**
     * 单个期望目标的相似度打分。
     */
    private SimilarityResult evaluateSingleSimilarity(String actual, String expected, double threshold) {
        Map<String, Integer> expectedTf = extractTermFrequencies(expected);
        Map<String, Integer> actualTf = extractTermFrequencies(actual);

        if (expectedTf.isEmpty() || actualTf.isEmpty()) {
            return new SimilarityResult(false, 0.0, threshold, "未能提取出有效特征词，相似度为 0.0%。", "", 0.0, 0.0, 0.0);
        }

        // 1. 全局主题余弦相似度
        double globalCosine = computeCosineSimilarity(expectedTf, actualTf);

        // 2. 期望核心概念覆盖率 (Recall)
        double coverage = computeConceptCoverage(expectedTf, actualTf);

        // 3. 局部最佳段落/句子契合度 (Sliding Window Segment Match)
        String[] segments = splitSentences(actual);
        double maxLocalCosine = 0.0;
        String bestSegment = "";
        for (String seg : segments) {
            String trimmed = seg.trim();
            if (trimmed.length() < 2) continue;
            Map<String, Integer> segTf = extractTermFrequencies(trimmed);
            if (segTf.isEmpty()) continue;
            double sim = computeCosineSimilarity(expectedTf, segTf);
            if (sim > maxLocalCosine) {
                maxLocalCosine = sim;
                bestSegment = trimmed;
            }
        }

        // 4. 综合相似度得分融合：局部最佳段落 45% + 核心概念覆盖率 35% + 全局主题相似度 20%
        double combinedScore = (0.45 * maxLocalCosine) + (0.35 * coverage) + (0.20 * globalCosine);
        combinedScore = Math.min(1.0, Math.max(0.0, combinedScore));

        boolean passed = combinedScore >= threshold;
        String scorePercent = String.format(java.util.Locale.ROOT, "%.1f%%", combinedScore * 100);
        String thresholdPercent = String.format(java.util.Locale.ROOT, "%.1f%%", threshold * 100);

        String message;
        if (passed) {
            message = String.format(java.util.Locale.ROOT,
                    "相似度分析达标：综合相似度 %s (达到判定阈值 %s)。核心段落契合度 %.1f%%，概念覆盖率 %.1f%%。",
                    scorePercent, thresholdPercent, maxLocalCosine * 100, coverage * 100);
        } else {
            message = String.format(java.util.Locale.ROOT,
                    "相似度分析未达标：综合相似度 %s (低于判定阈值 %s)。核心段落契合度 %.1f%%，概念覆盖率 %.1f%%。",
                    scorePercent, thresholdPercent, maxLocalCosine * 100, coverage * 100);
        }

        return new SimilarityResult(passed, combinedScore, threshold, message, bestSegment,
                maxLocalCosine, coverage, globalCosine);
    }

    /**
     * 提取文本特征词频（支持中文单字、双字 Bigram 与英文词汇）。
     */
    private Map<String, Integer> extractTermFrequencies(String text) {
        Map<String, Integer> tf = new java.util.HashMap<>();
        if (text == null || text.isBlank()) {
            return tf;
        }
        String clean = text.toLowerCase().replaceAll("[\\p{Punct}\\p{IsPunctuation}，。！？；：“”‘’（）《》【】、—…\\r\\n\\t]+", " ");
        String[] tokens = clean.split("\\s+");
        for (String token : tokens) {
            token = token.trim();
            if (token.isEmpty()) {
                continue;
            }
            if (token.matches("^[a-z0-9_-]+$")) {
                if (!STOP_WORDS.contains(token) && token.length() > 1) {
                    tf.merge(token, 1, Integer::sum);
                }
                continue;
            }
            for (int i = 0; i < token.length(); i++) {
                String singleChar = String.valueOf(token.charAt(i));
                if (!STOP_WORDS.contains(singleChar)) {
                    tf.merge(singleChar, 1, Integer::sum);
                }
                if (i + 1 < token.length()) {
                    String biChar = token.substring(i, i + 2);
                    if (!STOP_WORDS.contains(biChar)) {
                        tf.merge(biChar, 2, Integer::sum);
                    }
                }
            }
        }
        return tf;
    }

    /**
     * 计算两个词频向量的余弦相似度。
     */
    private double computeCosineSimilarity(Map<String, Integer> tf1, Map<String, Integer> tf2) {
        if (tf1.isEmpty() || tf2.isEmpty()) {
            return 0.0;
        }
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (Map.Entry<String, Integer> entry : tf1.entrySet()) {
            double val = entry.getValue();
            norm1 += val * val;
            if (tf2.containsKey(entry.getKey())) {
                dotProduct += val * tf2.get(entry.getKey());
            }
        }
        for (int val : tf2.values()) {
            norm2 += (double) val * val;
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * 计算期望核心概念覆盖率 (Recall / Coverage)。
     */
    private double computeConceptCoverage(Map<String, Integer> expectedTf, Map<String, Integer> actualTf) {
        if (expectedTf.isEmpty()) {
            return 0.0;
        }
        double matchedWeight = 0.0;
        double totalExpectedWeight = 0.0;

        for (Map.Entry<String, Integer> entry : expectedTf.entrySet()) {
            double weight = entry.getValue();
            totalExpectedWeight += weight;
            if (actualTf.containsKey(entry.getKey())) {
                matchedWeight += Math.min(weight, actualTf.get(entry.getKey()));
            }
        }
        return totalExpectedWeight == 0.0 ? 0.0 : Math.min(1.0, matchedWeight / totalExpectedWeight);
    }

    /**
     * 文本句子/段落切分。
     */
    private String[] splitSentences(String text) {
        if (text == null || text.isBlank()) {
            return new String[0];
        }
        return text.split("[\\n\\r。！？；!?;]+");
    }

    /**
     * 相似度评估诊断结果 Record。
     */
    private record SimilarityResult(
            boolean passed,
            double score,
            double threshold,
            String message,
            String bestSegment,
            double localScore,
            double coverageScore,
            double globalScore
    ) { }

    /**
     * 序列化对象为 JSON 字符串。
     */
    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "{}";
        }
    }

    /**
     * 安全解析指标适用性配置字符串为 JsonNode。
     */
    private JsonNode readApplicability(String value) {
        try {
            return value == null || value.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(value);
        } catch (Exception exception) {
            return objectMapper.createObjectNode();
        }
    }

    /**
     * 解析字符串为 JsonNode 对象。
     */
    private JsonNode readJson(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (Exception exception) {
            throw new IllegalStateException("评测快照 JSON 格式无效，无法解析。", exception);
        }
    }

    /**
     * 判定指标在指定规范中是否适用。
     */
    private boolean applicable(JsonNode value, String metric) {
        return !value.has(metric) || value.path(metric).asBoolean(true);
    }

    /**
     * 粗略估算 Token 消耗数。
     */
    private long estimateTokens(String value) {
        return value == null || value.isBlank() ? 0L : Math.max(1L, (value.length() + 3L) / 4L);
    }

    /**
     * 生成文本摘要（SHA-256 Hex）。
     */
    private String digest(String value) {
        try {
            String safeValue = value == null ? "" : value;
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(safeValue.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("评测输入输出摘要生成失败", exception);
        }
    }

    /** 评测样例证据状态 Record */
    private record EvaluationEvidence(EvaluationMetricState correctness,
                                      EvaluationMetricState groundedness,
                                      EvaluationMetricState citation,
                                      EvaluationMetricState cost) { }

    /** 评测运行聚合统计报告 Record */
    private record EvaluationAggregateReport(int caseCount, int succeededCount, double successRate,
                                             double averageLatencyMs, BigDecimal estimatedCost,
                                             int baselineSucceededCount, Double baselineSuccessRate,
                                             Double successRateDelta, List<Long> regressedCaseIds) { }
}

