package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.model.ApplicationWizardStep;
import com.acme.agentstudio.domain.application.model.ApplicationDraftConfiguration;
import com.acme.agentstudio.domain.application.model.ApplicationDraftResult;
import com.acme.agentstudio.domain.application.model.ApplicationWizardDefinition;
import com.acme.agentstudio.domain.workflow.model.ExecutionType;
import com.acme.agentstudio.domain.workflow.model.GraphDefinition;
import com.acme.agentstudio.domain.workflow.model.GraphEdge;
import com.acme.agentstudio.domain.workflow.model.GraphNode;
import com.acme.agentstudio.application.workflow.OrchestrationApplicationService;
import com.acme.agentstudio.application.application.ApplicationEntrypointService;
import com.acme.agentstudio.application.application.ApplicationContracts.ProtocolField;
import com.acme.agentstudio.domain.application.ApplicationEntrypointContracts.SaveEntrypointRequest;
import com.acme.agentstudio.domain.application.ApplicationEntrypointType;
import com.acme.agentstudio.domain.application.EntrypointVersionPolicy;
import com.acme.agentstudio.domain.application.RunDeliveryMode;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.infrastructure.persistence.entity.OrchestrationAppEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.OrchestrationAppMapper;
import com.acme.agentstudio.interfaces.rest.dto.OrchestrationDraftRequest;
import com.acme.agentstudio.infrastructure.workflow.GraphDefinitionParser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 应用创建向导服务。
 * 负责引导新手与高级开发者完成 AI 应用从基本元数据定义、输入输出契约规范到默认流程图拓扑生成的闭环。
 */
@Service
public class ApplicationCreationWizardService {

    /** 应用名称字段常量 */
    private static final String FIELD_NAME = "name";

    /** 业务目标字段常量 */
    private static final String FIELD_GOAL = "goal";

    /** 输入名称字段常量 */
    private static final String FIELD_INPUT_NAME = "inputName";

    /** 输入类型字段 */
    private static final String FIELD_INPUT_TYPE = "inputType";

    /** 输出名称字段常量 */
    private static final String FIELD_OUTPUT_NAME = "outputName";

    /** 输出格式字段常量 */
    private static final String FIELD_OUTPUT_FORMAT = "outputFormat";

    /** 默认应用状态：草稿 */
    private static final String DEFAULT_STATUS = "DRAFT";

    /** 默认画布 Schema 版本号 */
    private static final String GRAPH_SCHEMA_VERSION = "1.0";

    /** 开始节点标识 */
    private static final String NODE_START = "START";

    /** 结束节点标识 */
    private static final String NODE_END = "END";

    /** 默认端口标识 */
    private static final String DEFAULT_PORT = "default";

    /** 自动生成应用编码的前缀 */
    private static final String APP_CODE_PREFIX = "APP-";

    /** 随机生成的编码后缀长度 */
    private static final int GENERATED_CODE_LENGTH = 12;

    /** 编排应用数据库 Mapper */
    private final OrchestrationAppMapper appMapper;

    /** 编排应用应用服务 */
    private final OrchestrationApplicationService orchestrationService;

    /** 图拓扑定义解析器 */
    private final GraphDefinitionParser graphParser;

    /** Jackson JSON 序列化工具 */
    private final ObjectMapper objectMapper;

    /** 应用入口定义服务 */
    private final ApplicationEntrypointService entrypointService;

    /**
     * 构造函数注入依赖项。
     */
    public ApplicationCreationWizardService(OrchestrationAppMapper appMapper,
                                            OrchestrationApplicationService orchestrationService,
                                            GraphDefinitionParser graphParser,
                                            ObjectMapper objectMapper,
                                            ApplicationEntrypointService entrypointService) {
        this.appMapper = appMapper;
        this.orchestrationService = orchestrationService;
        this.graphParser = graphParser;
        this.objectMapper = objectMapper;
        this.entrypointService = entrypointService;
    }

    /**
     * 获取应用创建向导的步骤模板与交互字段定义。
     *
     * @param advanced 是否为高级向导模式
     * @return 包含向导步骤与校验规则的定义结构
     */
    public ApplicationWizardDefinition definition(boolean advanced) {
        List<ApplicationWizardStep> steps = List.of(
                new ApplicationWizardStep("identity", "应用信息", "给团队成员看的业务名称和说明",
                        List.of(FIELD_NAME), Map.of(), List.of(), false),
                new ApplicationWizardStep("goal", "业务目标", "说明应用需要解决的业务问题",
                        List.of(FIELD_GOAL), Map.of(), List.of(), false),
                new ApplicationWizardStep("input", "输入契约", "定义流程接收的业务输入",
                        List.of(FIELD_INPUT_NAME, FIELD_INPUT_TYPE), Map.of(FIELD_INPUT_NAME, "request", FIELD_INPUT_TYPE, "string"), List.of(), false),
                new ApplicationWizardStep("output", "输出契约", "定义流程交付的业务结果",
                        List.of(FIELD_OUTPUT_NAME, FIELD_OUTPUT_FORMAT),
                        Map.of(FIELD_OUTPUT_NAME, "result", FIELD_OUTPUT_FORMAT, "text"), List.of(), false)
        );
        return new ApplicationWizardDefinition(advanced, steps, true, !advanced);
    }

    /**
     * 根据向导配置在同一事务中创建应用元数据及其默认主工作流草稿。
     *
     * @param user 当前登录安全用户
     * @param name 应用名称
     * @param configuration 应用草稿配置参数
     * @return 包含新建应用 ID 与 AppCode 的创建结果
     */
    @Transactional
    public ApplicationDraftResult createDraft(SecurityUser user, String name,
                                               ApplicationDraftConfiguration configuration) {
        if (user == null || user.getTenantId() == null || user.getUserId() == null) {
            throw new IllegalArgumentException("当前身份无效");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("应用名称不能为空");
        }

        // 1. 生成全局唯一且规格化的应用编码
        String appCode = generateApplicationCode(user.getTenantId());
        LocalDateTime now = LocalDateTime.now();

        // 2. 持久化应用基础信息表
        OrchestrationAppEntity application = new OrchestrationAppEntity();
        application.setTenantId(user.getTenantId());
        application.setAppCode(appCode);
        application.setAppName(name.trim());
        application.setGraphType(ExecutionType.APPLICATION_WORKFLOW);
        application.setStatus(DEFAULT_STATUS);
        application.setCreatedBy(user.getUserId());
        application.setCreatedAt(now);
        application.setUpdatedAt(now);
        appMapper.insert(application);

        // 3. 应用和默认主工作流在同一事务中创建，避免出现有应用但没有执行过程的半成品
        orchestrationService.saveDraft(user, new OrchestrationDraftRequest(
                application.getId(), appCode, application.getAppName(), ExecutionType.APPLICATION_WORKFLOW,
                defaultGraphJson(configuration), null, "创建应用空白主工作流"));

        // 4. 初始化对话入口与表单入口
        createDefaultEntrypoints(user, application.getId(), configuration);

        return new ApplicationDraftResult(application.getId(), appCode, application.getAppName(),
                DEFAULT_STATUS, true);
    }

    /**
     * 为新建应用建立可发现的内部入口定义。
     */
    private void createDefaultEntrypoints(SecurityUser user, Long applicationId,
                                          ApplicationDraftConfiguration configuration) {
        ApplicationDraftConfiguration resolved = configuration == null
                ? ApplicationDraftConfiguration.defaults() : configuration;
        List<ProtocolField> fields = List.of(new ProtocolField(resolved.resolvedInputName(), resolved.resolvedInputType(), true,
                resolved.resolvedGoal()));
        entrypointService.create(user, applicationId, new SaveEntrypointRequest("对话入口",
                ApplicationEntrypointType.CONVERSATION, EntrypointVersionPolicy.FOLLOW_PRODUCTION,
                null, RunDeliveryMode.REALTIME, fields, null, null, true));
        entrypointService.create(user, applicationId, new SaveEntrypointRequest("表单入口",
                ApplicationEntrypointType.FORM, EntrypointVersionPolicy.FOLLOW_PRODUCTION,
                null, RunDeliveryMode.IMMEDIATE, fields, null, null, false));
    }

    /**
     * 生成默认包含 START 与 END 节点的初始流程图拓扑 JSON。
     */
    private String defaultGraphJson(ApplicationDraftConfiguration configuration) {
        ApplicationDraftConfiguration resolved = configuration == null
                ? ApplicationDraftConfiguration.defaults() : configuration;
        GraphDefinition graph = new GraphDefinition(
                ExecutionType.APPLICATION_WORKFLOW,
                GRAPH_SCHEMA_VERSION,
                inputSchema(resolved.resolvedInputName(), resolved.resolvedInputType(), resolved.resolvedGoal()),
                outputSchema(resolved.resolvedOutputName(), resolved.resolvedOutputFormat()),
                defaultNodes(),
                defaultEdges(),
                List.of());
        try {
            return objectMapper.writeValueAsString(graphParser.write(graph));
        } catch (Exception exception) {
            throw new IllegalStateException("默认主工作流生成失败，请稍后重试。", exception);
        }
    }

    /** 辅助构造默认的 START 和 END 节点列表 */
    private List<GraphNode> defaultNodes() {
        ObjectNode empty = JsonNodeFactory.instance.objectNode();
        return List.of(
                new GraphNode("start", NODE_START, "开始", null, null, empty.deepCopy(), empty.deepCopy(), empty.deepCopy()),
                new GraphNode("end", NODE_END, "结束", null, null, empty.deepCopy(), empty.deepCopy(), empty.deepCopy()));
    }

    /** 辅助构造从 START 到 END 的默认连线 */
    private List<GraphEdge> defaultEdges() {
        return List.of(
                new GraphEdge("edge-start-end", "start", DEFAULT_PORT, "end", DEFAULT_PORT));
    }

    /** 构造输入的 JSON Schema */
    private ObjectNode inputSchema(String inputName, String inputType, String businessGoal) {
        ObjectNode schema = baseObjectSchema();
        schema.put("description", businessGoal);
        schema.withObject("properties").putObject(inputName).put("type", inputType);
        schema.putArray("required").add(inputName);
        return schema;
    }

    /** 构造输出的 JSON Schema */
    private ObjectNode outputSchema(String outputName, String outputFormat) {
        ObjectNode schema = baseObjectSchema();
        ObjectNode output = schema.withObject("properties").putObject(outputName);
        output.put("type", "json".equalsIgnoreCase(outputFormat) ? "object" : "string");
        output.put("format", outputFormat);
        return schema;
    }

    /** 基础 Object 类型 Schema */
    private ObjectNode baseObjectSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.putObject("properties");
        return schema;
    }

    /** 循环生成租户内不重复的应用编码 */
    private String generateApplicationCode(Long tenantId) {
        String code;
        do {
            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, GENERATED_CODE_LENGTH);
            code = APP_CODE_PREFIX + suffix;
        } while (appMapper.selectCount(new LambdaQueryWrapper<OrchestrationAppEntity>()
                .eq(OrchestrationAppEntity::getTenantId, tenantId)
                .eq(OrchestrationAppEntity::getAppCode, code)) > 0);
        return code;
    }
}

