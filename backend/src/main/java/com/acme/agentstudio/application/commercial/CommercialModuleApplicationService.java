package com.acme.agentstudio.application.commercial;

import com.acme.agentstudio.application.task.PersistentTaskQueueService;
import com.acme.agentstudio.application.workflow.PersistentOrchestrationExecutionService;
import com.acme.agentstudio.common.response.PageQuery;
import com.acme.agentstudio.common.response.PageResult;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.common.ApplicationMessages;
import com.acme.agentstudio.domain.knowledge.model.RagRetrievalRequest;
import com.acme.agentstudio.domain.knowledge.model.RagSearchResult;
import com.acme.agentstudio.domain.knowledge.model.RetrievalLanguageStrategy;
import com.acme.agentstudio.domain.knowledge.model.RetrievalScopeType;
import com.acme.agentstudio.infrastructure.model.SmartModelRouter;
import com.acme.agentstudio.infrastructure.persistence.entity.AgentProfileEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.ApprovalTaskEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.AuditLogEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.EvaluationResultEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.EvaluationSampleEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.EvaluationTaskEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.MarketplaceItemEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.TenantEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.WorkflowDefinitionEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.AgentProfileMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.ApprovalTaskMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.AuditLogMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.EvaluationResultMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.EvaluationSampleMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.EvaluationTaskMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.MarketplaceItemMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.TenantMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.WorkflowDefinitionMapper;
import com.acme.agentstudio.infrastructure.prompt.PromptRenderService;
import com.acme.agentstudio.infrastructure.rag.RagAnswerGuard;
import com.acme.agentstudio.infrastructure.rag.RagRetrievalMetricService;
import com.acme.agentstudio.infrastructure.rag.RagRetrievalService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * 商业化运营管理应用服务类。
 * 负责租户资源配置、风控审核审批（Human-in-the-loop）、Agent 效果指标评测（Evaluation）、应用集市预置能力模版一键部署及行为审计报表的业务流程控制。
 */
@Service
public class CommercialModuleApplicationService {

    private static final Logger log = LoggerFactory.getLogger(CommercialModuleApplicationService.class);

    /** 工作流持久化图引擎运行服务 */
    @Autowired
    @Lazy
    private PersistentOrchestrationExecutionService orchestrationExecutionService;

    /** JSON 序列化映射工具 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 租户 Persistence Mapper */
    private final TenantMapper tenantMapper;

    /** 审批工单 Persistence Mapper */
    private final ApprovalTaskMapper approvalTaskMapper;

    /** 评测任务 Persistence Mapper */
    private final EvaluationTaskMapper evaluationTaskMapper;

    /** 评测测试用例样本 Persistence Mapper */
    private final EvaluationSampleMapper evaluationSampleMapper;

    /** 应用集市预置模板 Persistence Mapper */
    private final MarketplaceItemMapper marketplaceItemMapper;

    /** 审计日志 Persistence Mapper */
    private final AuditLogMapper auditLogMapper;

    /** Agent 档案 Persistence Mapper */
    private final AgentProfileMapper agentProfileMapper;

    /** 工作流定义 Persistence Mapper */
    private final WorkflowDefinitionMapper workflowDefinitionMapper;

    /** 评测测试结果明细 Persistence Mapper */
    private final EvaluationResultMapper evaluationResultMapper;

    /** 智能大模型路由组件 */
    private final SmartModelRouter smartModelRouter;

    /** Prompt 渲染服务 */
    private final PromptRenderService promptRenderService;

    /** RAG 知识检索服务 */
    private final RagRetrievalService ragRetrievalService;

    /** RAG 检索指标统计服务 */
    private final RagRetrievalMetricService ragRetrievalMetricService;

    /** 异步评测任务线程池执行器 */
    private final Executor evaluationTaskExecutor;

    /** 持久化后台任务队列 */
    private final PersistentTaskQueueService taskQueueService;

    /**
     * 构造函数注入商业化运营服务所需的全量持久化与组件依赖。
     */
    public CommercialModuleApplicationService(
            TenantMapper tenantMapper,
            ApprovalTaskMapper approvalTaskMapper,
            EvaluationTaskMapper evaluationTaskMapper,
            EvaluationSampleMapper evaluationSampleMapper,
            MarketplaceItemMapper marketplaceItemMapper,
            AuditLogMapper auditLogMapper,
            AgentProfileMapper agentProfileMapper,
            WorkflowDefinitionMapper workflowDefinitionMapper,
            EvaluationResultMapper evaluationResultMapper,
            SmartModelRouter smartModelRouter,
            PromptRenderService promptRenderService,
            RagRetrievalService ragRetrievalService,
            RagRetrievalMetricService ragRetrievalMetricService,
            @Qualifier("evaluationTaskExecutor") Executor evaluationTaskExecutor,
            PersistentTaskQueueService taskQueueService
    ) {
        this.tenantMapper = tenantMapper;
        this.approvalTaskMapper = approvalTaskMapper;
        this.evaluationTaskMapper = evaluationTaskMapper;
        this.evaluationSampleMapper = evaluationSampleMapper;
        this.marketplaceItemMapper = marketplaceItemMapper;
        this.auditLogMapper = auditLogMapper;
        this.agentProfileMapper = agentProfileMapper;
        this.workflowDefinitionMapper = workflowDefinitionMapper;
        this.evaluationResultMapper = evaluationResultMapper;
        this.smartModelRouter = smartModelRouter;
        this.promptRenderService = promptRenderService;
        this.ragRetrievalService = ragRetrievalService;
        this.ragRetrievalMetricService = ragRetrievalMetricService;
        this.evaluationTaskExecutor = evaluationTaskExecutor;
        this.taskQueueService = taskQueueService;
    }

    /**
     * 查询全系统所有已注册的租户列表（超级管理员全平台视图）。
     *
     * @return 全平台租户详细信息映射列表
     */
    public List<Map<String, Object>> listTenants() {
        return tenantMapper.selectList(null).stream()
                .map(this::tenantToMap)
                .toList();
    }

    /**
     * 查询指定租户的待办与历史人工审批工单列表。
     *
     * @param tenantId 租户 ID
     * @return 审批工单映射列表
     */
    public List<Map<String, Object>> listApprovals(Long tenantId) {
        requireTenantId(tenantId);
        return approvalTaskMapper.selectList(tenantQuery(tenantId)).stream()
                .map(this::approvalToMap)
                .toList();
    }

    /**
     * 按租户 ID、用户 ID 及角色资源权限过滤查询允许访问的工单列表。
     *
     * @param tenantId 租户 ID
     * @param userId 用户 ID
     * @param role 角色编码
     * @return 过滤后的工单映射列表
     */
    public List<Map<String, Object>> listApprovals(Long tenantId, Long userId, String role) {
        if ("ADMIN".equals(role) || "SUPER_ADMIN".equals(role)) {
            return listApprovals(tenantId);
        }
        return approvalTaskMapper.selectList(tenantQuery(tenantId)).stream()
                .map(this::approvalToMap).toList();
    }

    /**
     * 统一待办任务收件箱（Task Inbox）分页查询。
     *
     * @param tenantId 租户 ID
     * @param userId 用户 ID
     * @param role 角色编码
     * @param query 分页请求对象
     * @param type 任务类型过滤条件
     * @return 分页任务结果
     */
    public PageResult<Map<String, Object>> listTaskInbox(Long tenantId, Long userId, String role, PageQuery query, String type) {
        List<Map<String, Object>> items = new ArrayList<>();
        listApprovals(tenantId, userId, role).stream()
                .filter(item -> type == null || type.isBlank() || taskType(item).equalsIgnoreCase(type))
                .map(item -> {
                    Map<String, Object> task = new LinkedHashMap<>(item);
                    task.put("taskType", taskType(item));
                    task.put("nextAction", "PENDING".equals(item.get("status")) ? "APPROVE" : "VIEW");
                    task.put("source", "APPROVAL");
                    return task;
                }).forEach(items::add);
        items.sort(Comparator.comparing(item -> String.valueOf(item.getOrDefault("id", "")), Comparator.reverseOrder()));
        long total = items.size();
        int from = (int) Math.min(query.offset(), total);
        int to = Math.min(from + query.size(), items.size());
        return PageResult.of(items.subList(from, to), total, query);
    }

    /**
     * 内部方法：从工单标题识别判定其任务子类型（SUPPLEMENT / TRANSFER / APPROVAL）。
     */
    private String taskType(Map<String, Object> item) {
        String title = String.valueOf(item.getOrDefault("title", ""));
        if (title.contains("补充") || title.toLowerCase().contains("supplement")) {
            return "SUPPLEMENT";
        }
        if (title.contains("转交") || title.toLowerCase().contains("transfer")) {
            return "TRANSFER";
        }
        return "APPROVAL";
    }

    /**
     * 获取指定租户提交的智能体效果评测任务列表。
     *
     * @param tenantId 租户 ID
     * @return 评测任务对象列表
     */
    public List<Map<String, Object>> listEvaluations(Long tenantId) {
        requireTenantId(tenantId);
        return evaluationTaskMapper.selectList(tenantQuery(tenantId)).stream()
                .map(this::evaluationToMap)
                .toList();
    }

    /**
     * 获取租户维护的标准评测测试样本（包含问题与标准答案）。
     *
     * @param tenantId 租户 ID
     * @return 评测测试样本实体列表
     */
    public List<EvaluationSampleEntity> listEvaluationSamples(Long tenantId) {
        requireTenantId(tenantId);
        return evaluationSampleMapper.selectList(tenantQuery(tenantId));
    }

    /**
     * 新增标准评测测试用例样本。
     *
     * @param tenantId 租户 ID
     * @param question 测试问题
     * @param expectedAnswer 预期标准答案
     */
    public void createEvaluationSample(Long tenantId, String question, String expectedAnswer) {
        requireTenantId(tenantId);
        if (question == null || question.isBlank() || expectedAnswer == null || expectedAnswer.isBlank()) {
            throw new IllegalArgumentException("评测问题和期望答案不能为空。");
        }
        EvaluationSampleEntity sample = new EvaluationSampleEntity();
        sample.setTenantId(tenantId);
        sample.setQuestion(question.trim());
        sample.setExpectedAnswer(expectedAnswer.trim());
        sample.setCreatedAt(LocalDateTime.now());
        evaluationSampleMapper.insert(sample);
    }

    /**
     * 获取应用集市中公开的所有 Agent 和 Workflow 预制应用模板。
     *
     * @return 应用市场模版映射列表
     */
    public List<Map<String, Object>> listMarketplaceItems() {
        return marketplaceItemMapper.selectList(null).stream()
                .map(this::marketplaceToMap)
                .toList();
    }

    /**
     * 获取指定租户名下的所有管理员操作及安全防爆破拦截审计日志。
     *
     * @param tenantId 租户 ID
     * @return 审计事件日志映射列表
     */
    public List<Map<String, Object>> listAuditEvents(Long tenantId) {
        requireTenantId(tenantId);
        return auditLogMapper.selectList(tenantQuery(tenantId)).stream()
                .map(this::auditToMap)
                .toList();
    }

    /**
     * 执行人工审批工单决策（同意或驳回），并在通过后自动唤醒恢复挂起的工作流。
     *
     * @param id 工单 ID
     * @param tenantId 租户 ID
     * @param userId 当前操作用户 ID
     * @param operatorId 操作人标识
     * @param role 角色编码
     * @param action 决策结果（APPROVED / REJECTED）
     * @param comment 审批意见说明
     */
    public void approveTask(Long id, Long tenantId, Long userId, String operatorId, String role, String action, String comment) {
        ApprovalTaskEntity task = approvalTaskMapper.selectById(id);
        if (task == null) {
            throw new IllegalArgumentException("未找到该审批任务：" + id);
        }
        if (!task.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("越权操作：审批任务不属于当前租户");
        }
        if (!"PENDING".equals(task.getApprovalStatus())) {
            throw new IllegalArgumentException("该审批任务已经处理，不能重复提交审批结果。");
        }
        if (!"APPROVED".equalsIgnoreCase(action) && !"REJECTED".equalsIgnoreCase(action)) {
            throw new IllegalArgumentException("审批结果只支持通过或驳回。");
        }
        if (comment == null || comment.isBlank()) {
            throw new IllegalArgumentException("审批意见不能为空。");
        }

        Map<String, Object> executionPayload = readApprovalPayload(task);
        if (!"ORCHESTRATION".equals(executionPayload.get("engine"))
                || !(executionPayload.get("executionId") instanceof String executionId) || executionId.isBlank()) {
            throw new IllegalStateException("该历史工作流已下线，无法继续处理审批任务。");
        }

        String decision = "APPROVED".equalsIgnoreCase(action) ? "APPROVED" : "REJECTED";
        if (approvalTaskMapper.markDecision(id, tenantId, decision) != 1) {
            throw new IllegalArgumentException("该审批任务已经被其他审批人处理，不能重复提交。");
        }
        task.setApprovalStatus(decision);
        task.setUpdatedAt(LocalDateTime.now());

        AuditLogEntity audit = new AuditLogEntity();
        audit.setTenantId(tenantId);
        audit.setOperatorId(operatorId);
        audit.setActionType("APPROVE_TASK");
        audit.setTargetType("APPROVAL");
        audit.setTargetId(String.valueOf(id));
        audit.setRiskLevel("P2");

        try {
            Map<String, Object> payload = objectMapper.readValue(task.getPayloadJson(), Map.class);
            payload.put("approvalDecision", decision);
            payload.put("approvalComment", comment.trim());
            payload.put("approvalOperator", operatorId);
            payload.put("approvalAt", LocalDateTime.now().toString());
            task.setPayloadJson(objectMapper.writeValueAsString(payload));
            approvalTaskMapper.updateById(task);
        } catch (Exception exception) {
            throw new IllegalStateException("审批意见保存失败，未恢复工作流。", exception);
        }
        audit.setDetailJson(toJson(Map.of("action", action, "title", task.getTitle(), "comment", comment.trim())));
        audit.setCreatedAt(LocalDateTime.now());
        auditLogMapper.insert(audit);

        try {
            String resumeDecision = "APPROVED".equals(decision) ? "APPROVE" : "REJECT";
            orchestrationExecutionService.enqueueResume(
                    new SecurityUser(userId, tenantId, operatorId, role), executionId, resumeDecision);
        } catch (Exception e) {
            log.error("[审批引擎] 审批结果处理后恢复挂起工作流失败", e);
            throw new IllegalStateException("审批结果已保存，但工作流恢复失败，请查看执行记录并人工接管。", e);
        }
    }

    /**
     * 提交创建新的 Agent 效果基准评测任务。
     *
     * @param tenantId 租户 ID
     * @param agentId 目标 Agent ID
     * @param taskName 评测任务名称
     * @param sampleCount 使用的样本测试数量
     * @param scoringRule 打分匹配规则（EXACT_MATCH 精确匹配 / CONTAINS_EXPECTED 包含匹配）
     */
    public void createEvaluationTask(Long tenantId, Long agentId, String taskName, Integer sampleCount, String scoringRule) {
        requireTenantId(tenantId);
        if (agentId == null) {
            throw new IllegalArgumentException("评测任务必须绑定 Agent。");
        }
        if (taskName == null || taskName.isBlank()) {
            throw new IllegalArgumentException("评测集名称不能为空");
        }
        AgentProfileEntity agent = agentProfileMapper.selectById(agentId);
        if (agent == null || !tenantId.equals(agent.getTenantId())) {
            throw new IllegalArgumentException("评测任务绑定的 Agent 不属于当前租户。");
        }
        if (agent.getModelKey() == null || agent.getModelKey().isBlank() || agent.getPromptTemplate() == null || agent.getPromptTemplate().isBlank()) {
            throw new IllegalArgumentException("评测任务绑定的 Agent 尚未配置模型底座或 Prompt 模板。");
        }
        if (!Set.of("EXACT_MATCH", "CONTAINS_EXPECTED").contains(scoringRule)) {
            throw new IllegalArgumentException("评测评分规则不受支持。");
        }

        int availableSamples = evaluationSampleMapper.selectCount(tenantQuery(tenantId)).intValue();
        int requestedSamples = sampleCount == null ? availableSamples : sampleCount;
        if (requestedSamples <= 0 || requestedSamples > availableSamples) {
            throw new IllegalArgumentException("评测样本数量必须大于零且不能超过当前租户的真实样本数量。");
        }
        EvaluationTaskEntity task = new EvaluationTaskEntity();
        task.setTenantId(tenantId);
        task.setAgentId(agentId);
        task.setTaskName(taskName);
        task.setSampleCount(requestedSamples);
        task.setScoringRule(scoringRule);
        task.setEvaluationStatus("DRAFT");
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        evaluationTaskMapper.insert(task);

        taskQueueService.enqueue(tenantId, "EVALUATION", Map.of("taskId", task.getId()), 3);
    }

    /**
     * 查询指定评测任务产生的单条样本测试结果得分与延迟数据。
     *
     * @param taskId 评测任务 ID
     * @param tenantId 租户 ID
     * @return 评测明细结果实体列表
     */
    public List<EvaluationResultEntity> listEvaluationResults(Long taskId, Long tenantId) {
        requireTenantId(tenantId);
        EvaluationTaskEntity task = evaluationTaskMapper.selectById(taskId);
        if (task == null || !tenantId.equals(task.getTenantId())) {
            throw new IllegalArgumentException("未找到当前租户的评测任务：" + taskId);
        }
        return evaluationResultMapper.selectList(new LambdaQueryWrapper<EvaluationResultEntity>().eq(EvaluationResultEntity::getTaskId, taskId).orderByAsc(EvaluationResultEntity::getId));
    }

    /**
     * 对已失败的评测任务重新入队重试执行。
     *
     * @param taskId 评测任务 ID
     * @param tenantId 租户 ID
     */
    public void retryEvaluation(Long taskId, Long tenantId) {
        requireTenantId(tenantId);
        EvaluationTaskEntity task = evaluationTaskMapper.selectById(taskId);
        if (task == null || !tenantId.equals(task.getTenantId())) {
            throw new IllegalArgumentException("未找到当前租户的评测任务：" + taskId);
        }
        if (!"FAILED".equals(task.getEvaluationStatus())) {
            throw new IllegalArgumentException("只有执行失败的评测任务可以重试。");
        }
        evaluationResultMapper.delete(new QueryWrapper<EvaluationResultEntity>().eq("task_id", taskId));
        task.setScore(null);
        task.setReportJson(null);
        task.setEvaluationStatus("DRAFT");
        task.setUpdatedAt(LocalDateTime.now());
        evaluationTaskMapper.updateById(task);
        taskQueueService.enqueue(tenantId, "EVALUATION", Map.of("taskId", taskId), 3);
    }

    /**
     * 执行真实智能体效果评测，批量生成回答并打分。
     *
     * @param taskId 评测任务 ID
     */
    public void executeEvaluation(Long taskId) {
        EvaluationTaskEntity task = evaluationTaskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        if (evaluationTaskMapper.markRunning(taskId) != 1) {
            return;
        }
        List<EvaluationSampleEntity> samples = evaluationSampleMapper.selectList(
                new QueryWrapper<EvaluationSampleEntity>()
                        .eq("tenant_id", task.getTenantId())
                        .orderByAsc("id")
                        .last("LIMIT " + task.getSampleCount())
        );
        AgentProfileEntity agent = agentProfileMapper.selectById(task.getAgentId());
        List<Map<String, Object>> details = new ArrayList<>();
        BigDecimal totalScore = BigDecimal.ZERO;
        int successCount = 0;
        try {
            ChatLanguageModel model = smartModelRouter.route(task.getTenantId(), agent.getModelKey(), "评测任务");
            for (EvaluationSampleEntity sample : samples) {
                EvaluationResultEntity result = new EvaluationResultEntity();
                result.setTaskId(taskId);
                result.setSampleId(sample.getId());
                result.setQuestion(sample.getQuestion());
                result.setExpectedAnswer(sample.getExpectedAnswer());
                long startedAt = System.nanoTime();
                try {
                    List<RagSearchResult> references = ragRetrievalService.retrieveWithContext(
                            new RagRetrievalRequest(
                                    task.getTenantId(), null, sample.getQuestion(),
                                    RetrievalLanguageStrategy.AUTO,
                                    null,
                                    RetrievalScopeType.VISIBLE_DOCUMENTS,
                                    List.of(), 5, null)).results();
                    double topScore = references.stream().mapToDouble(RagSearchResult::score).max().orElse(0D);
                    String actualAnswer;
                    if (!RagAnswerGuard.hasHits(references)) {
                        actualAnswer = RagAnswerGuard.NO_HIT_REPLY;
                        ragRetrievalMetricService.record(task.getTenantId(), "EVALUATION", 0, 0D, false);
                    } else {
                        String prompt = promptRenderService.render(agent, sample.getQuestion(), references);
                        actualAnswer = RagAnswerGuard.enforce(model.generate(prompt), references);
                        ragRetrievalMetricService.record(
                                task.getTenantId(),
                                "EVALUATION",
                                references.size(),
                                topScore,
                                RagAnswerGuard.isGrounded(actualAnswer, references)
                        );
                    }
                    BigDecimal sampleScore = score(task.getScoringRule(), actualAnswer, sample.getExpectedAnswer());
                    result.setActualAnswer(actualAnswer);
                    result.setScore(sampleScore);
                    result.setResultStatus("SUCCESS");
                    successCount++;
                    totalScore = totalScore.add(sampleScore);
                } catch (Exception exception) {
                    result.setResultStatus("FAILED");
                    result.setErrorMessage(exception.getMessage());
                }
                result.setLatencyMs((System.nanoTime() - startedAt) / 1_000_000);
                result.setCreatedAt(LocalDateTime.now());
                evaluationResultMapper.insert(result);
                details.add(mapOf("sampleId", sample.getId(), "status", result.getResultStatus(), "score", result.getScore(), "latencyMs", result.getLatencyMs(), "errorMessage", result.getErrorMessage()));
            }
            task.setScore(successCount == 0 ? null : totalScore.divide(BigDecimal.valueOf(successCount), 3, RoundingMode.HALF_UP));
            task.setEvaluationStatus(successCount == samples.size() ? "COMPLETED" : "FAILED");
            task.setReportJson(objectMapper.writeValueAsString(mapOf("scoringRule", task.getScoringRule(), "sampleCount", samples.size(), "successCount", successCount, "details", details)));
        } catch (Exception exception) {
            task.setEvaluationStatus("FAILED");
            try {
                task.setReportJson(objectMapper.writeValueAsString(mapOf("errorMessage", exception.getMessage(), "details", details)));
            } catch (Exception ignored) {
                task.setReportJson(null);
            }
        }
        task.setUpdatedAt(LocalDateTime.now());
        evaluationTaskMapper.updateById(task);
    }

    /**
     * 根据评分规则与预期答案，计算单测试样本的得分（满分 100 / 零分 0）。
     */
    private BigDecimal score(String scoringRule, String actualAnswer, String expectedAnswer) {
        String actual = normalizeAnswer(actualAnswer);
        String expected = normalizeAnswer(expectedAnswer);
        boolean matched = "EXACT_MATCH".equals(scoringRule) ? actual.equals(expected) : actual.contains(expected);
        return matched ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
    }

    /**
     * 规范化消除空格与大小写以匹配对比。
     */
    private String normalizeAnswer(String answer) {
        return answer == null ? "" : answer.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    /**
     * 一键从集市安装部署 Agent 或 Workflow 预置能力到当前租户工作区。
     *
     * @param tenantId 租户 ID
     * @param itemName 集市模版名称
     * @param itemType 模版类型（Agent 或 Workflow）
     * @param operatorId 操作人标识
     */
    public void installMarketplaceItem(Long tenantId, String itemName, String itemType, String operatorId) {
        requireTenantId(tenantId);

        AuditLogEntity audit = new AuditLogEntity();
        audit.setTenantId(tenantId);
        audit.setOperatorId(operatorId);
        audit.setActionType("INSTALL_TEMPLATE");
        audit.setTargetType("MARKETPLACE");
        audit.setTargetId(itemName);
        audit.setRiskLevel("P3");
        audit.setDetailJson("{\"itemType\":\"" + itemType + "\"}");
        audit.setCreatedAt(LocalDateTime.now());
        auditLogMapper.insert(audit);

        MarketplaceItemEntity template = marketplaceItemMapper.selectOne(
                new LambdaQueryWrapper<MarketplaceItemEntity>()
                        .eq(MarketplaceItemEntity::getItemName, itemName)
                        .eq(MarketplaceItemEntity::getItemType, itemType)
                        .eq(MarketplaceItemEntity::getItemStatus, "READY")
        );
        if (template == null || template.getManifestJson() == null || template.getManifestJson().isBlank()) {
            throw new IllegalArgumentException("能力市场模板缺少可安装清单，无法部署。");
        }

        try {
            JsonNode manifest = objectMapper.readTree(template.getManifestJson());
            if ("Agent".equalsIgnoreCase(itemType)) {
                String modelKey = manifest.path("modelKey").asText("");
                String promptTemplate = manifest.path("promptTemplate").asText("");
                if (modelKey.isBlank() || promptTemplate.isBlank()) {
                    throw new IllegalArgumentException("Agent 模板清单必须配置模型编码和 Prompt 模板。");
                }
                AgentProfileEntity agent = new AgentProfileEntity();
                agent.setTenantId(tenantId);
                agent.setAgentCode("installed-" + UUID.randomUUID().toString().replace("-", ""));
                agent.setAgentName(itemName);
                agent.setAgentType("CHAT");
                agent.setOwnerTeam("能力市场");
                agent.setPromptTemplate(promptTemplate);
                agent.setModelKey(modelKey);
                agent.setStatus("DRAFT");
                agent.setCreatedAt(LocalDateTime.now());
                agent.setUpdatedAt(LocalDateTime.now());
                agentProfileMapper.insert(agent);
            } else if ("Workflow".equalsIgnoreCase(itemType)) {
                String graphJson = manifest.path("graphJson").asText("");
                if (graphJson.isBlank()) {
                    throw new IllegalArgumentException("工作流模板清单必须配置工作流图定义。");
                }
                WorkflowDefinitionEntity workflow = new WorkflowDefinitionEntity();
                workflow.setTenantId(tenantId);
                workflow.setWorkflowCode("wf-installed-" + UUID.randomUUID().toString().replace("-", ""));
                workflow.setWorkflowName(itemName);
                workflow.setVersionNo(1);
                workflow.setStatus("DRAFT");
                workflow.setGraphJson(graphJson);
                workflow.setCreatedAt(LocalDateTime.now());
                workflow.setUpdatedAt(LocalDateTime.now());
                workflowDefinitionMapper.insert(workflow);
            }
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("能力市场模板清单格式不合法。", exception);
        }
    }

    /**
     * 转化 TenantEntity 为前端 Display Map 结构。
     */
    private Map<String, Object> tenantToMap(TenantEntity item) {
        return mapOf("id", item.getId(), "name", item.getTenantName(), "code", item.getTenantCode(), "plan", item.getPlanCode(), "users", item.getUserLimit(), "status", item.getStatus());
    }

    /**
     * 转化 ApprovalTaskEntity 为前端 Display Map 结构。
     */
    private Map<String, Object> approvalToMap(ApprovalTaskEntity item) {
        String description = "";
        String request = "";
        String analysis = "";
        String ragContext = "";
        try {
            Map<?, ?> payload = objectMapper.readValue(item.getPayloadJson(), Map.class);
            Object value = payload.get("approvalDescription");
            description = value == null ? "" : String.valueOf(value);
            request = textValue(payload.get("businessRequest"));
            analysis = textValue(payload.get("lastLlmOutput"));
            ragContext = textValue(payload.get("rag_context"));
        } catch (Exception ignored) {
        }
        return mapOf("id", item.getId(), "title", item.getTitle(), "description", description,
                "request", request, "analysis", analysis, "ragContext", ragContext,
                "owner", item.getOwnerTeam(), "risk", item.getRiskLevel(), "status", item.getApprovalStatus());
    }

    /**
     * 从工单实体反序列化获取 Payload json 上下文 Map。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> readApprovalPayload(ApprovalTaskEntity task) {
        try {
            return objectMapper.readValue(task.getPayloadJson(), Map.class);
        } catch (Exception exception) {
            throw new IllegalArgumentException("审批任务上下文格式无效，无法恢复工作流。", exception);
        }
    }

    /**
     * 提取字段字符串值辅助函数。
     */
    private String textValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * 序列化 Map 为 JSON 辅助函数。
     */
    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "{}";
        }
    }

    /**
     * 转化 EvaluationTaskEntity 为 Map。
     */
    private Map<String, Object> evaluationToMap(EvaluationTaskEntity item) {
        return mapOf("id", item.getId(), "name", item.getTaskName(), "samples", item.getSampleCount(), "agentId", item.getAgentId(), "scoringRule", item.getScoringRule(),
                "score", item.getScore() == null ? null : item.getScore().toPlainString(),
                "status", item.getEvaluationStatus(), "report", item.getReportJson());
    }

    /**
     * 转化 MarketplaceItemEntity 为 Map。
     */
    private Map<String, Object> marketplaceToMap(MarketplaceItemEntity item) {
        return mapOf("id", item.getId(), "name", item.getItemName(), "type", item.getItemType(),
                "publisher", item.getPublisher(), "status", item.getItemStatus());
    }

    /**
     * 转化 AuditLogEntity 为 Map。
     */
    private Map<String, Object> auditToMap(AuditLogEntity item) {
        return mapOf("action", item.getActionType(), "actor", item.getOperatorId(), "target", item.getTargetType() + ":" + item.getTargetId(), "level", item.getRiskLevel());
    }

    /**
     * 键值对变长参数构建 Map 的 LinkedHashMap 辅助函数。
     */
    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            result.put(String.valueOf(values[i]), values[i + 1]);
        }
        return result;
    }

    /**
     * 辅助校验 tenantId 必填。
     */
    private void requireTenantId(Long tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException(ApplicationMessages.TENANT_ID_REQUIRED);
        }
    }

    /**
     * 快速生成包含 tenant_id 的 MyBatis QueryWrapper。
     */
    private <T> QueryWrapper<T> tenantQuery(Long tenantId) {
        return new QueryWrapper<T>().eq("tenant_id", tenantId);
    }
}

