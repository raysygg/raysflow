package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.application.workflow.ExecutionEventType;
import com.acme.agentstudio.application.workflow.ExecutionStatus;
import com.acme.agentstudio.application.workflow.LlmNodeExecutor;
import com.acme.agentstudio.domain.runtime.model.RuntimeContext;
import com.acme.agentstudio.domain.runtime.model.RuntimeOutput;
import com.acme.agentstudio.infrastructure.persistence.entity.PlatformExecutionContextEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.PlatformExecutionEventEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.PlatformExecutionContextMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.PlatformExecutionEventMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 会话运行环境持久化执行应用服务（Chat Runtime Execution Service）。
 * 负责 Chat 模式下运行上下文的持久化初始化、模型调用调度、结构化响应解析（Structured Output）、
 * 知识库引用归属（Citations）构造以及事件轨迹（RUN_STARTED/RUN_COMPLETED）落地。
 */
@Service
public class ChatRuntimeExecutionService {

    /** 运行执行类型常量：CHAT */
    private static final String EXECUTION_TYPE_CHAT = "CHAT";

    /** 事件类型常量：运行开始 */
    private static final String EVENT_RUN_STARTED = "RUN_STARTED";

    /** 事件类型常量：上下文已构建 */
    private static final String EVENT_CONTEXT_BUILT = "CONTEXT_BUILT";

    /** 事件类型常量：模型已调用 */
    private static final String EVENT_MODEL_CALLED = "MODEL_CALLED";

    /** 事件类型常量：运行已完成 */
    private static final String EVENT_RUN_COMPLETED = "RUN_COMPLETED";

    /** 事件类型常量：运行失败 */
    private static final String EVENT_RUN_FAILED = "RUN_FAILED";

    /** 变量 Key：input */
    private static final String INPUT_KEY = "input";

    /** 变量 Key：query */
    private static final String QUERY_KEY = "query";

    /** 变量 Key：user_message */
    private static final String USER_MESSAGE_KEY = "user_message";

    /** 配置 Key：结构化输出 Schema */
    private static final String STRUCTURED_SCHEMA_KEY = "structuredOutputSchema";

    /** 事件序号：运行开始 */
    private static final long SEQUENCE_RUN_STARTED = 0L;

    /** 事件序号：上下文就绪 */
    private static final long SEQUENCE_CONTEXT_BUILT = 1L;

    /** 事件序号：动作起始 */
    private static final long SEQUENCE_FIRST_RUNTIME_ACTION = 2L;

    /** 运行上下文持久化 Mapper */
    private final PlatformExecutionContextMapper contextMapper;

    /** 运行轨迹事件 Mapper */
    private final PlatformExecutionEventMapper eventMapper;

    /** 大语言模型节点执行器 */
    private final LlmNodeExecutor llmNodeExecutor;

    /** Jackson JSON 序列化工具 */
    private final ObjectMapper objectMapper;

    /**
     * 构造函数注入持久化与模型执行依赖。
     */
    public ChatRuntimeExecutionService(
            PlatformExecutionContextMapper contextMapper,
            PlatformExecutionEventMapper eventMapper,
            LlmNodeExecutor llmNodeExecutor,
            ObjectMapper objectMapper
    ) {
        this.contextMapper = contextMapper;
        this.eventMapper = eventMapper;
        this.llmNodeExecutor = llmNodeExecutor;
        this.objectMapper = objectMapper;
    }

    /**
     * 执行会话 Runtime 对话流程，生成轨迹事件并记录执行结果。
     *
     * @param context 会话运行上下文 RuntimeContext
     * @param configuration 附加的运行配置参数 Map
     * @return 包含执行 ID、状态与输出的响应 Map
     */
    @Transactional
    public Map<String, Object> execute(RuntimeContext context, Map<String, Object> configuration) {
        if (context == null) {
            throw new IllegalArgumentException("Chat Runtime 运行上下文不能为空。");
        }
        PlatformExecutionContextEntity existing = contextMapper.selectOne(new LambdaQueryWrapper<PlatformExecutionContextEntity>()
                .eq(PlatformExecutionContextEntity::getTenantId, context.tenantId())
                .eq(PlatformExecutionContextEntity::getIdempotencyKey, context.runId()));
        if (existing != null) {
            return result(existing);
        }

        PlatformExecutionContextEntity execution = new PlatformExecutionContextEntity();
        execution.setTenantId(context.tenantId());
        execution.setExecutionId(context.runId());
        execution.setAppId(context.applicationId());
        execution.setVersionId(parseVersionId(context.releaseId()));
        execution.setExecutionType(EXECUTION_TYPE_CHAT);
        execution.setIdempotencyKey(context.runId());
        execution.setStatus(ExecutionStatus.RUNNING);
        execution.setInputJson(write(context.input()));
        execution.setStartedAt(LocalDateTime.now());
        contextMapper.insert(execution);

        appendEvent(context.tenantId(), context.runId(), EVENT_RUN_STARTED, SEQUENCE_RUN_STARTED, Map.of("mode", context.mode().name()));
        appendEvent(context.tenantId(), context.runId(), EVENT_CONTEXT_BUILT, SEQUENCE_CONTEXT_BUILT,
                Map.of("releaseId", context.releaseId(), "budget", context.budget()));

        long sequence = SEQUENCE_FIRST_RUNTIME_ACTION;
        try {
            Map<String, Object> variables = new LinkedHashMap<>(context.variables());
            Object input = context.input().getOrDefault(INPUT_KEY, context.input().getOrDefault(QUERY_KEY, ""));
            variables.putIfAbsent(INPUT_KEY, input);
            variables.putIfAbsent(QUERY_KEY, input);
            variables.putIfAbsent(USER_MESSAGE_KEY, input);

            ObjectNode nodeConfig = objectMapper.valueToTree(configuration == null ? Map.of() : configuration);
            LlmNodeExecutor.Result modelResult = llmNodeExecutor.execute(context.tenantId(), nodeConfig, variables);

            appendEvent(context.tenantId(), context.runId(), EVENT_MODEL_CALLED, sequence++,
                    Map.of("modelKey", modelResult.modelKey(), "attempt", modelResult.attempt()));
            appendEvent(context.tenantId(), context.runId(), ExecutionEventType.LLM_CONTEXT_RESOLVED, sequence++,
                    Map.of("contextUsage", modelResult.contextUsage()));

            RuntimeOutput output = output(modelResult.text(), nodeConfig.path(STRUCTURED_SCHEMA_KEY), context.retrievedKnowledge());
            execution.setOutputJson(write(output));
            execution.setStatus(ExecutionStatus.SUCCEEDED);
            execution.setFinishedAt(LocalDateTime.now());
            contextMapper.updateById(execution);

            appendEvent(context.tenantId(), context.runId(), EVENT_RUN_COMPLETED, sequence, Map.of("answer", output.text()));
            return result(execution);
        } catch (Exception exception) {
            execution.setStatus(ExecutionStatus.FAILED);
            execution.setErrorCode("CHAT_RUNTIME_FAILED");
            execution.setErrorMessage(exception.getMessage() == null ? "Chat Runtime 对话流程执行失败" : exception.getMessage());
            execution.setFinishedAt(LocalDateTime.now());
            contextMapper.updateById(execution);

            appendEvent(context.tenantId(), context.runId(), EVENT_RUN_FAILED, sequence,
                    Map.of("error", execution.getErrorMessage()));
            throw new IllegalStateException("Chat Runtime 对话流程执行失败，详情请查阅异常跟踪。", exception);
        }
    }

    /** 构造运行输出模型与解析结构化 JSON */
    private RuntimeOutput output(String text, JsonNode schema, Map<String, Object> knowledge) {
        Map<String, Object> structured = new LinkedHashMap<>();
        if (schema != null && !schema.isMissingNode() && !schema.isNull() && !schema.isEmpty()) {
            try {
                Map<String, Object> parsed = objectMapper.readValue(
                        text,
                        objectMapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Object.class)
                );
                structured.putAll(parsed);
            } catch (Exception exception) {
                throw new IllegalArgumentException("模型生成的结构化输出不是合法的 JSON 格式字符串。", exception);
            }
        }
        return new RuntimeOutput(text, structured, citations(knowledge), true);
    }

    /** 提取知识库检索文档的 Citations 列表 */
    private List<RuntimeOutput.Citation> citations(Map<String, Object> knowledge) {
        Object raw = knowledge == null ? null : knowledge.get("results");
        if (!(raw instanceof Iterable<?> results)) {
            return List.of();
        }
        return java.util.stream.StreamSupport.stream(results.spliterator(), false)
                .filter(item -> item instanceof Map<?, ?>)
                .map(item -> (Map<?, ?>) item)
                .filter(item -> item.get("sourceId") != null)
                .map(item -> new RuntimeOutput.Citation(
                        String.valueOf(item.get("sourceId")),
                        String.valueOf(value(item, "title", item.get("sourceId"))),
                        String.valueOf(value(item, "excerpt", "")),
                        item.get("score") instanceof Number number ? number.doubleValue() : 0D
                ))
                .toList();
    }

    /** 读取 Map 属性值或返回默认兜底对象 */
    private Object value(Map<?, ?> values, String key, Object fallback) {
        return values.containsKey(key) ? values.get(key) : fallback;
    }

    /** 解析 Release ID 转换成 Long 类型版本号 */
    private Long parseVersionId(String releaseId) {
        try {
            return releaseId == null ? null : Long.valueOf(releaseId);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /** 持久化插入单条轨迹事件 */
    private void appendEvent(long tenantId, String executionId, String eventType, long sequence, Object payload) {
        PlatformExecutionEventEntity event = new PlatformExecutionEventEntity();
        event.setTenantId(tenantId);
        event.setExecutionId(executionId);
        event.setEventType(eventType);
        event.setSequenceNo(sequence);
        event.setPayloadJson(write(payload));
        event.setCreatedAt(LocalDateTime.now());
        eventMapper.insert(event);
    }

    /** 构建标准输出响应 Map */
    private Map<String, Object> result(PlatformExecutionContextEntity execution) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("executionId", execution.getExecutionId());
        result.put("status", execution.getStatus());
        result.put("output", execution.getOutputJson());
        result.put("errorCode", execution.getErrorCode());
        result.put("errorMessage", execution.getErrorMessage());
        return result;
    }

    /** 写入 JSON 字符串工具方法 */
    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Chat Runtime 运行结果 JSON 序列化失败。", exception);
        }
    }
}

