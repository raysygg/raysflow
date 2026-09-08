package com.acme.agentstudio.application.lifecycle;

import com.acme.agentstudio.application.task.PersistentTaskQueueService;
import com.acme.agentstudio.application.task.TaskType;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.lifecycle.ApplicationLifecycleContracts.CandidateStatus;
import com.acme.agentstudio.domain.lifecycle.ApplicationLifecycleContracts.EvaluationStatus;
import com.acme.agentstudio.domain.lifecycle.ApplicationLifecycleException;
import com.acme.agentstudio.infrastructure.persistence.entity.ApplicationEvaluationCaseEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.ApplicationEvaluationResultEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.ApplicationEvaluationRunEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.ApplicationEvaluationSuiteEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.ApplicationEvaluationSuiteVersionEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.OrchestrationAppEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.ApplicationEvaluationCaseMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.ApplicationEvaluationResultMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.ApplicationEvaluationRunMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.ApplicationEvaluationSuiteMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.ApplicationEvaluationSuiteVersionMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.OrchestrationAppMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Comparator;

/**
 * AI 应用自动化评测集与评测任务生命周期管理服务。
 * 负责评测测试用例集（Suite / Suite Version / Case）的定义与版本控制、发起针对 Candidate 快照的评测任务（Run）、推入后台持久化任务队列异步执行并提供评测报告与指标证据查询。
 */
@Service
public class ApplicationEvaluationService {

    /** 状态：活动 */
    private static final String ACTIVE = "ACTIVE";

    /** 状态：适用 */
    private static final String APPLICABLE = "APPLICABLE";

    /** 评测集 Mapper */
    private final ApplicationEvaluationSuiteMapper suiteMapper;

    /** 评测集版本 Mapper */
    private final ApplicationEvaluationSuiteVersionMapper versionMapper;

    /** 评测测试用例 Mapper */
    private final ApplicationEvaluationCaseMapper caseMapper;

    /** 评测 Run 运行任务 Mapper */
    private final ApplicationEvaluationRunMapper runMapper;

    /** 评测结果证据 Mapper */
    private final ApplicationEvaluationResultMapper resultMapper;

    /** 候选发布版本服务 */
    private final ReleaseCandidateService candidateService;

    /** JSON 序列化映射工具 */
    private final ObjectMapper objectMapper;

    /** 持久化异步任务队列服务 */
    private final PersistentTaskQueueService taskQueue;

    /** 应用归属查询 Mapper */
    private final OrchestrationAppMapper appMapper;

    /**
     * 构造函数注入评测管理服务依赖组件。
     */
    public ApplicationEvaluationService(ApplicationEvaluationSuiteMapper suiteMapper,
                                         ApplicationEvaluationSuiteVersionMapper versionMapper,
                                         ApplicationEvaluationCaseMapper caseMapper,
                                         ApplicationEvaluationRunMapper runMapper,
                                         ApplicationEvaluationResultMapper resultMapper,
                                         ReleaseCandidateService candidateService,
                                         ObjectMapper objectMapper,
                                         PersistentTaskQueueService taskQueue,
                                         OrchestrationAppMapper appMapper) {
        this.suiteMapper = suiteMapper;
        this.versionMapper = versionMapper;
        this.caseMapper = caseMapper;
        this.runMapper = runMapper;
        this.resultMapper = resultMapper;
        this.candidateService = candidateService;
        this.objectMapper = objectMapper;
        this.taskQueue = taskQueue;
        this.appMapper = appMapper;
    }

    /**
     * 为指定 AI 应用创建一套全新的测试评测集及初始第 1 版用例列表。
     *
     * @param user 当前登录用户
     * @param applicationId 应用 ID
     * @param request 创建评测集请求参数
     * @return 评测集创建结果契约
     */
    @Transactional
    public SuiteCreated createSuite(SecurityUser user, Long applicationId, CreateSuiteRequest request) {
        requireIdentity(user);
        requireApplication(user, applicationId);
        if (request == null || request.suiteCode() == null || request.suiteCode().isBlank()) {
            throw error("SUITE_INVALID", "评测集编码不能为空。");
        }
        if (request.cases() == null || request.cases().isEmpty()) {
            throw error("SUITE_CASES_REQUIRED", "评测集至少需要一个测试用例。");
        }
        ApplicationEvaluationSuiteEntity suite = new ApplicationEvaluationSuiteEntity();
        suite.setTenantId(user.getTenantId());
        suite.setApplicationId(applicationId);
        suite.setSuiteCode(request.suiteCode().trim());
        suite.setSuiteName(request.suiteName());
        suite.setSuiteStatus(ACTIVE);
        suite.setCreatedBy(user.getUserId());
        suite.setCreatedAt(LocalDateTime.now());
        suite.setUpdatedAt(suite.getCreatedAt());
        suiteMapper.insert(suite);

        ApplicationEvaluationSuiteVersionEntity version = new ApplicationEvaluationSuiteVersionEntity();
        version.setTenantId(user.getTenantId());
        version.setApplicationId(applicationId);
        version.setSuiteId(suite.getId());
        version.setVersionNo(1);
        version.setScoringPolicyJson(json(request.scoringPolicy()));
        version.setVersionStatus(ACTIVE);
        version.setCreatedBy(user.getUserId());
        version.setCreatedAt(LocalDateTime.now());
        versionMapper.insert(version);

        for (int index = 0; index < request.cases().size(); index++) {
            EvaluationCaseRequest item = request.cases().get(index);
            ApplicationEvaluationCaseEntity evaluationCase = new ApplicationEvaluationCaseEntity();
            evaluationCase.setTenantId(user.getTenantId());
            evaluationCase.setApplicationId(applicationId);
            evaluationCase.setSuiteVersionId(version.getId());
            evaluationCase.setCaseCode(item.caseCode());
            evaluationCase.setInputJson(json(item.input()));
            evaluationCase.setExpectedRuleJson(json(item.expectedRule()));
            evaluationCase.setMetricApplicabilityJson(item.metricApplicability() == null ? APPLICABLE : json(item.metricApplicability()));
            evaluationCase.setSortOrder(index);
            evaluationCase.setCreatedAt(LocalDateTime.now());
            caseMapper.insert(evaluationCase);
        }
        return new SuiteCreated(suite.getId(), version.getId(), request.cases().size());
    }

    /** 查询应用下的命名评测集，并返回其最新不可变版本。 */
    public List<EvaluationSuiteSummary> listSuites(SecurityUser user, Long applicationId) {
        requireIdentity(user);
        requireApplication(user, applicationId);
        return suiteMapper.selectList(new LambdaQueryWrapper<ApplicationEvaluationSuiteEntity>()
                        .eq(ApplicationEvaluationSuiteEntity::getTenantId, user.getTenantId())
                        .eq(ApplicationEvaluationSuiteEntity::getApplicationId, applicationId)
                        .orderByDesc(ApplicationEvaluationSuiteEntity::getUpdatedAt))
                .stream().map(suite -> {
                    ApplicationEvaluationSuiteVersionEntity latest = versionMapper.selectList(new LambdaQueryWrapper<ApplicationEvaluationSuiteVersionEntity>()
                                    .eq(ApplicationEvaluationSuiteVersionEntity::getTenantId, user.getTenantId())
                                    .eq(ApplicationEvaluationSuiteVersionEntity::getApplicationId, applicationId)
                                    .eq(ApplicationEvaluationSuiteVersionEntity::getSuiteId, suite.getId()))
                            .stream().max(Comparator.comparing(ApplicationEvaluationSuiteVersionEntity::getVersionNo)).orElse(null);
                    long caseCount = latest == null ? 0 : caseMapper.selectCount(new LambdaQueryWrapper<ApplicationEvaluationCaseEntity>()
                            .eq(ApplicationEvaluationCaseEntity::getTenantId, user.getTenantId())
                            .eq(ApplicationEvaluationCaseEntity::getApplicationId, applicationId)
                            .eq(ApplicationEvaluationCaseEntity::getSuiteVersionId, latest.getId()));
                    return new EvaluationSuiteSummary(suite.getId(), suite.getSuiteCode(), suite.getSuiteName(), suite.getSuiteStatus(),
                            latest == null ? null : latest.getId(), latest == null ? null : latest.getVersionNo(), caseCount);
                }).toList();
    }

    /** 查询评测集的不可变版本列表。 */
    public List<EvaluationSuiteVersionSummary> listVersions(SecurityUser user, Long applicationId, Long suiteId) {
        ApplicationEvaluationSuiteEntity suite = requireSuite(user, applicationId, suiteId);
        return versionMapper.selectList(new LambdaQueryWrapper<ApplicationEvaluationSuiteVersionEntity>()
                        .eq(ApplicationEvaluationSuiteVersionEntity::getTenantId, user.getTenantId())
                        .eq(ApplicationEvaluationSuiteVersionEntity::getApplicationId, applicationId)
                        .eq(ApplicationEvaluationSuiteVersionEntity::getSuiteId, suite.getId())
                        .orderByDesc(ApplicationEvaluationSuiteVersionEntity::getVersionNo))
                .stream().map(version -> new EvaluationSuiteVersionSummary(version.getId(), version.getSuiteId(), version.getVersionNo(),
                        version.getVersionStatus(), version.getCreatedAt(), caseMapper.selectCount(new LambdaQueryWrapper<ApplicationEvaluationCaseEntity>()
                        .eq(ApplicationEvaluationCaseEntity::getTenantId, user.getTenantId())
                        .eq(ApplicationEvaluationCaseEntity::getApplicationId, applicationId)
                        .eq(ApplicationEvaluationCaseEntity::getSuiteVersionId, version.getId())))).toList();
    }

    /** 查询不可变评测集版本的样例。 */
    public List<EvaluationCaseDetail> listCases(SecurityUser user, Long applicationId, Long suiteVersionId) {
        requireVersion(user, applicationId, suiteVersionId);
        return caseMapper.selectList(new LambdaQueryWrapper<ApplicationEvaluationCaseEntity>()
                        .eq(ApplicationEvaluationCaseEntity::getTenantId, user.getTenantId())
                        .eq(ApplicationEvaluationCaseEntity::getApplicationId, applicationId)
                        .eq(ApplicationEvaluationCaseEntity::getSuiteVersionId, suiteVersionId)
                        .orderByAsc(ApplicationEvaluationCaseEntity::getSortOrder))
                .stream().map(item -> new EvaluationCaseDetail(item.getId(), item.getCaseCode(), readJson(item.getInputJson()),
                        readJson(item.getExpectedRuleJson()), readJson(item.getMetricApplicabilityJson()), item.getSortOrder())).toList();
    }

    /** 基于新样例创建下一个不可变评测集版本。 */
    @Transactional
    public SuiteCreated createVersion(SecurityUser user, Long applicationId, Long suiteId, CreateSuiteVersionRequest request) {
        ApplicationEvaluationSuiteEntity suite = requireSuite(user, applicationId, suiteId);
        if (request == null || request.cases() == null || request.cases().isEmpty()) {
            throw error("SUITE_CASES_REQUIRED", "评测集版本至少需要一个测试样例。");
        }
        int nextVersion = versionMapper.selectList(new LambdaQueryWrapper<ApplicationEvaluationSuiteVersionEntity>()
                        .eq(ApplicationEvaluationSuiteVersionEntity::getTenantId, user.getTenantId())
                        .eq(ApplicationEvaluationSuiteVersionEntity::getApplicationId, applicationId)
                        .eq(ApplicationEvaluationSuiteVersionEntity::getSuiteId, suiteId))
                .stream().map(ApplicationEvaluationSuiteVersionEntity::getVersionNo).max(Integer::compareTo).orElse(0) + 1;
        ApplicationEvaluationSuiteVersionEntity version = new ApplicationEvaluationSuiteVersionEntity();
        version.setTenantId(user.getTenantId());
        version.setApplicationId(applicationId);
        version.setSuiteId(suiteId);
        version.setVersionNo(nextVersion);
        version.setScoringPolicyJson(json(request.scoringPolicy()));
        version.setVersionStatus(ACTIVE);
        version.setCreatedBy(user.getUserId());
        version.setCreatedAt(LocalDateTime.now());
        versionMapper.insert(version);
        insertCases(user, applicationId, version.getId(), request.cases());
        suite.setUpdatedAt(LocalDateTime.now());
        suiteMapper.updateById(suite);
        return new SuiteCreated(suiteId, version.getId(), request.cases().size());
    }

    /**
     * 发起针对特定候选版本（Candidate）的异步评测 Run 运行任务。
     *
     * @param user 当前登录用户
     * @param applicationId 应用 ID
     * @param request 创建评测 Run 请求参数
     * @return 评测运行摘要契约
     */
    @Transactional
    public EvaluationRunSummary createRun(SecurityUser user, Long applicationId, CreateEvaluationRunRequest request) {
        requireIdentity(user);
        if (request == null || request.candidateId() == null || request.suiteVersionId() == null) {
            throw error("EVALUATION_BINDING_REQUIRED", "Candidate 候选版本和 Suite 评测集版本不能为空。");
        }
        var candidate = candidateService.requireCandidate(user, applicationId, request.candidateId());
        if (!candidate.getSnapshotFingerprint().equals(request.candidateFingerprint())) {
            throw error("CANDIDATE_FINGERPRINT_MISMATCH", "评测指纹与候选版本快照指纹不一致。");
        }
        ApplicationEvaluationSuiteVersionEntity version = versionMapper.selectOne(new LambdaQueryWrapper<ApplicationEvaluationSuiteVersionEntity>()
                .eq(ApplicationEvaluationSuiteVersionEntity::getTenantId, user.getTenantId())
                .eq(ApplicationEvaluationSuiteVersionEntity::getApplicationId, applicationId)
                .eq(ApplicationEvaluationSuiteVersionEntity::getId, request.suiteVersionId()));
        if (version == null) {
            throw error("SUITE_VERSION_NOT_FOUND", "评测集版本不存在。");
        }

        ApplicationEvaluationRunEntity run = new ApplicationEvaluationRunEntity();
        run.setTenantId(user.getTenantId());
        run.setApplicationId(applicationId);
        run.setCandidateId(candidate.getId());
        run.setCandidateFingerprint(candidate.getSnapshotFingerprint());
        run.setSuiteVersionId(version.getId());
        run.setBaselineReleaseId(request.baselineReleaseId());
        run.setDependencyFingerprint(candidate.getSnapshotFingerprint());
        run.setEvaluationStatus(EvaluationStatus.QUEUED.name());
        run.setCreatedBy(user.getUserId());
        run.setCreatedAt(LocalDateTime.now());
        run.setUpdatedAt(run.getCreatedAt());
        runMapper.insert(run);

        taskQueue.enqueuePayload(
                user.getTenantId(),
                TaskType.APPLICATION_EVALUATION,
                new ApplicationEvaluationTaskPayload(
                        run.getId(),
                        user.getUserId(),
                        applicationId,
                        candidate.getId(),
                        candidate.getSnapshotFingerprint(),
                        version.getId()
                ),
                2
        );

        if (CandidateStatus.CREATED.name().equals(candidate.getCandidateStatus())) {
            candidateService.transition(user, applicationId, candidate.getId(), CandidateStatus.CREATED, CandidateStatus.EVALUATING);
        }
        return toSummary(run);
    }

    /**
     * 查询指定应用下的评测 Run 运行任务摘要列表。
     *
     * @param user 当前登录用户
     * @param applicationId 应用 ID
     * @return 评测运行摘要列表
     */
    public List<EvaluationRunSummary> listRuns(SecurityUser user, Long applicationId) {
        requireIdentity(user);
        return runMapper.selectList(new LambdaQueryWrapper<ApplicationEvaluationRunEntity>()
                        .eq(ApplicationEvaluationRunEntity::getTenantId, user.getTenantId())
                        .eq(ApplicationEvaluationRunEntity::getApplicationId, applicationId)
                        .orderByDesc(ApplicationEvaluationRunEntity::getCreatedAt))
                .stream().map(this::toSummary).toList();
    }

    /**
     * 查询指定评测 Run 任务的详细统计报告与逐用例结果证据。
     *
     * @param user 当前登录用户
     * @param applicationId 应用 ID
     * @param runId 评测 Run ID
     * @return 评测 Run 详情契约对象
     */
    public EvaluationRunDetail detail(SecurityUser user, Long applicationId, Long runId) {
        requireIdentity(user);
        ApplicationEvaluationRunEntity run = runMapper.selectOne(new LambdaQueryWrapper<ApplicationEvaluationRunEntity>()
                .eq(ApplicationEvaluationRunEntity::getTenantId, user.getTenantId())
                .eq(ApplicationEvaluationRunEntity::getApplicationId, applicationId)
                .eq(ApplicationEvaluationRunEntity::getId, runId));
        if (run == null) {
            throw error("EVALUATION_NOT_FOUND", "评测任务不存在。");
        }
        List<EvaluationCaseEvidence> evidence = resultMapper.selectList(new LambdaQueryWrapper<ApplicationEvaluationResultEntity>()
                        .eq(ApplicationEvaluationResultEntity::getTenantId, user.getTenantId())
                        .eq(ApplicationEvaluationResultEntity::getApplicationId, applicationId)
                        .eq(ApplicationEvaluationResultEntity::getEvaluationRunId, runId)
                        .orderByAsc(ApplicationEvaluationResultEntity::getCaseId))
                .stream().map(this::toEvidence).toList();
        return new EvaluationRunDetail(toSummary(run), readJson(run.getAggregateReportJson()), evidence);
    }

    /** 为部分失败或失败的评测创建全新的重试 Run，旧证据保持不变。 */
    @Transactional
    public EvaluationRunSummary retry(SecurityUser user, Long applicationId, Long runId) {
        requireIdentity(user);
        ApplicationEvaluationRunEntity previous = runMapper.selectOne(new LambdaQueryWrapper<ApplicationEvaluationRunEntity>()
                .eq(ApplicationEvaluationRunEntity::getTenantId, user.getTenantId())
                .eq(ApplicationEvaluationRunEntity::getApplicationId, applicationId)
                .eq(ApplicationEvaluationRunEntity::getId, runId));
        if (previous == null) throw error("EVALUATION_NOT_FOUND", "需要重试的评测任务不存在。");
        EvaluationStatus status = EvaluationStatus.valueOf(previous.getEvaluationStatus());
        if (status != EvaluationStatus.PARTIAL && status != EvaluationStatus.FAILED) {
            throw error("EVALUATION_NOT_RETRYABLE", "只有部分完成或失败的评测任务可以重试。");
        }
        return createRun(user, applicationId, new CreateEvaluationRunRequest(previous.getCandidateId(),
                previous.getCandidateFingerprint(), previous.getSuiteVersionId(), previous.getBaselineReleaseId()));
    }

    /**
     * 删除指定评测任务及关联的结果明细。
     *
     * @param user 当前登录用户
     * @param applicationId 应用 ID
     * @param runId 评测 Run ID
     */
    @Transactional
    public void deleteRun(SecurityUser user, Long applicationId, Long runId) {
        requireIdentity(user);
        requireApplication(user, applicationId);
        ApplicationEvaluationRunEntity run = runMapper.selectOne(new LambdaQueryWrapper<ApplicationEvaluationRunEntity>()
                .eq(ApplicationEvaluationRunEntity::getTenantId, user.getTenantId())
                .eq(ApplicationEvaluationRunEntity::getApplicationId, applicationId)
                .eq(ApplicationEvaluationRunEntity::getId, runId));
        if (run == null) {
            throw error("EVALUATION_NOT_FOUND", "评测任务不存在。");
        }
        // 删除关联的用例明细证据
        resultMapper.delete(new LambdaQueryWrapper<ApplicationEvaluationResultEntity>()
                .eq(ApplicationEvaluationResultEntity::getTenantId, user.getTenantId())
                .eq(ApplicationEvaluationResultEntity::getApplicationId, applicationId)
                .eq(ApplicationEvaluationResultEntity::getEvaluationRunId, runId));
        // 删除评测任务本体
        runMapper.deleteById(runId);
    }

    /**
     * 批量清理评测任务。
     *
     * @param user 当前登录用户
     * @param applicationId 应用 ID
     * @param scope 清理范围：'failed'（仅清理失败或未通过的）、'all'（清理全部）
     * @return 已清理的任务数量
     */
    @Transactional
    public int cleanRuns(SecurityUser user, Long applicationId, String scope) {
        requireIdentity(user);
        requireApplication(user, applicationId);
        LambdaQueryWrapper<ApplicationEvaluationRunEntity> wrapper = new LambdaQueryWrapper<ApplicationEvaluationRunEntity>()
                .eq(ApplicationEvaluationRunEntity::getTenantId, user.getTenantId())
                .eq(ApplicationEvaluationRunEntity::getApplicationId, applicationId);

        if (!"all".equalsIgnoreCase(scope)) {
            // 默认只清理未完全成功的任务（FAILED, PARTIAL, QUEUED 等）
            wrapper.ne(ApplicationEvaluationRunEntity::getEvaluationStatus, EvaluationStatus.SUCCEEDED.name());
        }

        List<ApplicationEvaluationRunEntity> list = runMapper.selectList(wrapper);
        if (list.isEmpty()) {
            return 0;
        }

        List<Long> runIds = list.stream().map(ApplicationEvaluationRunEntity::getId).toList();
        resultMapper.delete(new LambdaQueryWrapper<ApplicationEvaluationResultEntity>()
                .eq(ApplicationEvaluationResultEntity::getTenantId, user.getTenantId())
                .eq(ApplicationEvaluationResultEntity::getApplicationId, applicationId)
                .in(ApplicationEvaluationResultEntity::getEvaluationRunId, runIds));
        runMapper.deleteBatchIds(runIds);
        return runIds.size();
    }

    /**
     * 转换单个结果实体为用例证据契约。
     */
    private EvaluationCaseEvidence toEvidence(ApplicationEvaluationResultEntity result) {
        String inputContent = "";
        String expectedContent = "";
        String actualOutput = "";
        String matchDetails = "";
        Double similarityScore = null;
        String bestSegment = "";

        // 优先从持久化的 evidenceJson 中解析完整证据快照
        if (result.getEvidenceJson() != null && !result.getEvidenceJson().isBlank()) {
            try {
                JsonNode evidenceNode = objectMapper.readTree(result.getEvidenceJson());
                if (evidenceNode.hasNonNull("input")) {
                    inputContent = evidenceNode.get("input").asText("");
                }
                if (evidenceNode.hasNonNull("expected")) {
                    expectedContent = evidenceNode.get("expected").asText("");
                }
                if (evidenceNode.hasNonNull("actualOutput")) {
                    actualOutput = evidenceNode.get("actualOutput").asText("");
                }
                if (evidenceNode.hasNonNull("matchDetails")) {
                    matchDetails = evidenceNode.get("matchDetails").asText("");
                }
                if (evidenceNode.hasNonNull("similarityScore")) {
                    similarityScore = evidenceNode.get("similarityScore").asDouble();
                }
                if (evidenceNode.hasNonNull("bestSegment")) {
                    bestSegment = evidenceNode.get("bestSegment").asText("");
                }
            } catch (Exception ignored) {
            }
        }

        // 兼容历史数据：若未能获取到 input 或 expected，尝试从 Case 表回填
        if (inputContent.isBlank() || expectedContent.isBlank()) {
            try {
                ApplicationEvaluationCaseEntity caseEntity = caseMapper.selectById(result.getCaseId());
                if (caseEntity != null) {
                    if (inputContent.isBlank() && caseEntity.getInputJson() != null) {
                        inputContent = extractInputContent(caseEntity.getInputJson());
                    }
                    if (expectedContent.isBlank() && caseEntity.getExpectedRuleJson() != null) {
                        JsonNode rule = objectMapper.readTree(caseEntity.getExpectedRuleJson());
                        expectedContent = rule.path("expected").asText(rule.isTextual() ? rule.asText() : "");
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return new EvaluationCaseEvidence(
                result.getCaseId(),
                result.getEvaluationVariant(),
                result.getResultStatus(),
                result.getTaskSuccessScore(),
                result.getCorrectnessScore(),
                result.getGroundednessScore(),
                result.getCitationScore(),
                result.getLatencyMs(),
                result.getInputTokens(),
                result.getOutputTokens(),
                result.getEstimatedCost(),
                result.getFailureCategory(),
                result.getFailureMessage(),
                inputContent,
                expectedContent,
                actualOutput,
                matchDetails,
                similarityScore,
                bestSegment
        );
    }

    /**
     * 辅助解析用例输入文本。
     */
    private String extractInputContent(String inputJson) {
        try {
            JsonNode node = objectMapper.readTree(inputJson);
            if (node.isObject()) {
                if (node.hasNonNull("input")) return node.get("input").asText();
                if (node.hasNonNull("prompt")) return node.get("prompt").asText();
                if (node.hasNonNull("query")) return node.get("query").asText();
                var fields = node.fields();
                if (fields.hasNext()) return fields.next().getValue().asText();
            }
        } catch (Exception ignored) {
        }
        return inputJson;
    }

    /**
     * 解析字符串为 JsonNode。
     */
    private JsonNode readJson(String value) {
        if (value == null || value.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(value);
        } catch (Exception exception) {
            return objectMapper.createObjectNode();
        }
    }

    /** 批量写入某个不可变版本的样例快照。 */
    private void insertCases(SecurityUser user, Long applicationId, Long suiteVersionId,
                             List<EvaluationCaseRequest> cases) {
        for (int index = 0; index < cases.size(); index++) {
            EvaluationCaseRequest item = cases.get(index);
            ApplicationEvaluationCaseEntity evaluationCase = new ApplicationEvaluationCaseEntity();
            evaluationCase.setTenantId(user.getTenantId());
            evaluationCase.setApplicationId(applicationId);
            evaluationCase.setSuiteVersionId(suiteVersionId);
            evaluationCase.setCaseCode(item.caseCode());
            evaluationCase.setInputJson(json(item.input()));
            evaluationCase.setExpectedRuleJson(json(item.expectedRule()));
            evaluationCase.setMetricApplicabilityJson(item.metricApplicability() == null ? APPLICABLE : json(item.metricApplicability()));
            evaluationCase.setSortOrder(index);
            evaluationCase.setCreatedAt(LocalDateTime.now());
            caseMapper.insert(evaluationCase);
        }
    }

    /** 校验应用属于当前租户。 */
    private void requireApplication(SecurityUser user, Long applicationId) {
        OrchestrationAppEntity application = appMapper.selectOne(new LambdaQueryWrapper<OrchestrationAppEntity>()
                .eq(OrchestrationAppEntity::getTenantId, user.getTenantId())
                .eq(OrchestrationAppEntity::getId, applicationId));
        if (application == null) throw error("APPLICATION_NOT_FOUND", "应用不存在或不属于当前租户。");
    }

    /** 校验并返回应用下的评测集。 */
    private ApplicationEvaluationSuiteEntity requireSuite(SecurityUser user, Long applicationId, Long suiteId) {
        requireIdentity(user);
        requireApplication(user, applicationId);
        ApplicationEvaluationSuiteEntity suite = suiteMapper.selectOne(new LambdaQueryWrapper<ApplicationEvaluationSuiteEntity>()
                .eq(ApplicationEvaluationSuiteEntity::getTenantId, user.getTenantId())
                .eq(ApplicationEvaluationSuiteEntity::getApplicationId, applicationId)
                .eq(ApplicationEvaluationSuiteEntity::getId, suiteId));
        if (suite == null) throw error("SUITE_NOT_FOUND", "评测集不存在或不属于当前应用。");
        return suite;
    }

    /** 校验并返回应用下的评测集版本。 */
    private ApplicationEvaluationSuiteVersionEntity requireVersion(SecurityUser user, Long applicationId, Long versionId) {
        requireIdentity(user);
        requireApplication(user, applicationId);
        ApplicationEvaluationSuiteVersionEntity version = versionMapper.selectOne(new LambdaQueryWrapper<ApplicationEvaluationSuiteVersionEntity>()
                .eq(ApplicationEvaluationSuiteVersionEntity::getTenantId, user.getTenantId())
                .eq(ApplicationEvaluationSuiteVersionEntity::getApplicationId, applicationId)
                .eq(ApplicationEvaluationSuiteVersionEntity::getId, versionId));
        if (version == null) throw error("SUITE_VERSION_NOT_FOUND", "评测集版本不存在或不属于当前应用。");
        return version;
    }

    /**
     * 转换 Run 实体对象为摘要契约。
     */
    private EvaluationRunSummary toSummary(ApplicationEvaluationRunEntity run) {
        return new EvaluationRunSummary(
                run.getId(),
                run.getApplicationId(),
                run.getCandidateId(),
                run.getCandidateFingerprint(),
                run.getSuiteVersionId(),
                run.getBaselineReleaseId(),
                EvaluationStatus.valueOf(run.getEvaluationStatus()),
                run.getCreatedAt(),
                run.getCompletedAt(),
                run.getFailureCategory(),
                run.getFailureMessage()
        );
    }

    /**
     * 序列化对象为 JSON 字符串。
     */
    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? objectMapper.createObjectNode() : value);
        } catch (Exception exception) {
            throw error("EVALUATION_CONTRACT_INVALID", "评测契约无法序列化。");
        }
    }

    /**
     * 校验租户身份。
     */
    private void requireIdentity(SecurityUser user) {
        if (user == null || user.getTenantId() == null || user.getUserId() == null) {
            throw error("IDENTITY_INVALID", "当前身份无效。");
        }
    }

    /**
     * 构造 ApplicationLifecycleException。
     */
    private ApplicationLifecycleException error(String code, String message) {
        return new ApplicationLifecycleException(code, message);
    }

    /** 创建评测集请求 Record */
    public record CreateSuiteRequest(String suiteCode, String suiteName, JsonNode scoringPolicy,
                                     List<EvaluationCaseRequest> cases) { }

    /** 评测测试用例请求 Record */
    public record EvaluationCaseRequest(String caseCode, JsonNode input,
                                        JsonNode expectedRule,
                                        JsonNode metricApplicability) { }

    /** 创建评测任务请求 Record */
    public record CreateEvaluationRunRequest(Long candidateId, String candidateFingerprint, Long suiteVersionId, String baselineReleaseId) { }

    /** 评测集创建成功响应 Record */
    public record SuiteCreated(Long suiteId, Long suiteVersionId, int caseCount) { }

    /** 创建不可变评测集版本请求。 */
    public record CreateSuiteVersionRequest(JsonNode scoringPolicy, List<EvaluationCaseRequest> cases) { }

    /** 评测集及其最新版本摘要。 */
    public record EvaluationSuiteSummary(Long suiteId, String suiteCode, String suiteName, String status,
                                         Long latestVersionId, Integer latestVersionNo, long caseCount) { }

    /** 不可变评测集版本摘要。 */
    public record EvaluationSuiteVersionSummary(Long suiteVersionId, Long suiteId, Integer versionNo,
                                                String status, LocalDateTime createdAt, long caseCount) { }

    /** 评测样例详情。 */
    public record EvaluationCaseDetail(Long caseId, String caseCode, JsonNode input, JsonNode expectedRule,
                                       JsonNode metricApplicability, Integer sortOrder) { }

    /** 评测运行摘要 Record */
    public record EvaluationRunSummary(Long runId, Long applicationId, Long candidateId, String candidateFingerprint,
                                       Long suiteVersionId, String baselineReleaseId, EvaluationStatus status,
                                       LocalDateTime createdAt, LocalDateTime completedAt, String failureCategory, String failureMessage) { }
    /** 评测运行详情与整体报告 Record */
    public record EvaluationRunDetail(EvaluationRunSummary run, JsonNode aggregateReport,
                                      List<EvaluationCaseEvidence> cases) { }

    /** 单个用例证据与指标得分 Record */
    public record EvaluationCaseEvidence(Long caseId, String evaluationVariant, String status, BigDecimal taskSuccess,
                                         BigDecimal correctness, BigDecimal groundedness,
                                         BigDecimal citation, Long latencyMs, Long inputTokens,
                                         Long outputTokens, BigDecimal estimatedCost,
                                         String failureCategory, String failureMessage,
                                         String inputContent, String expectedContent,
                                         String actualOutput, String matchDetails,
                                         Double similarityScore, String bestSegment) { }

    /** 评测后台异步任务 Payload Record */
    public record ApplicationEvaluationTaskPayload(Long runId, Long userId, Long applicationId,
                                                   Long candidateId, String candidateFingerprint,
                                                   Long suiteVersionId) { }
}
