package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.application.workflow.PersistentOrchestrationExecutionService;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.runtime.model.RunStatus;
import com.acme.agentstudio.domain.runtime.model.RuntimeContext;
import com.acme.agentstudio.domain.runtime.model.RuntimeMode;
import com.acme.agentstudio.domain.runtime.model.RuntimeStrategyContracts;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 固定工作流编排运行模式策略适配器（Workflow Runtime Strategy）。
 * 实现 RuntimeStrategy 接口，绑定 RuntimeMode.WORKFLOW 模式。
 * 将控制流分发对接至底层 PersistentOrchestrationExecutionService 持久化编排执行引擎，
 * 支持工作流生命周期 start() 初始化事件生成及 enqueue() 异步 Task 任务入队。
 */
@Component
public class WorkflowRuntimeStrategy implements RuntimeStrategy {

    /** 编排执行类型标识 */
    private static final String EXECUTION_TYPE = "APPLICATION_WORKFLOW";

    /** 默认角色名称 */
    private static final String DEFAULT_ROLE = "USER";

    /** 持久化编排执行服务 */
    private final PersistentOrchestrationExecutionService executionService;

    /**
     * 构造函数注入依赖服务。
     *
     * @param executionService 持久化编排执行服务
     */
    public WorkflowRuntimeStrategy(PersistentOrchestrationExecutionService executionService) {
        this.executionService = executionService;
    }

    /**
     * 返回策略对应的运行模式 WORKFLOW。
     *
     * @return RuntimeMode.WORKFLOW
     */
    @Override
    public RuntimeMode mode() {
        return RuntimeMode.WORKFLOW;
    }

    /**
     * 返回固定工作流策略的能力描述 Descriptor（标记 ready = true，已生产就绪）。
     *
     * @return 策略能力描述符 Descriptor
     */
    @Override
    public RuntimeStrategyContracts.Descriptor descriptor() {
        return new RuntimeStrategyContracts.Descriptor(
                RuntimeMode.WORKFLOW,
                "固定工作流编排",
                "RuntimeContext.input",
                "PersistentOrchestrationExecutionService.output",
                "RuntimeContext.layers（包含输入参数、中间节点流程变量与 RAG 检索结果）",
                List.of(
                        RuntimeStrategyContracts.EventKind.STRATEGY_STARTED,
                        RuntimeStrategyContracts.EventKind.CONTEXT_RESOLVED,
                        RuntimeStrategyContracts.EventKind.STEP_STARTED,
                        RuntimeStrategyContracts.EventKind.STRATEGY_COMPLETED,
                        RuntimeStrategyContracts.EventKind.STRATEGY_FAILED
                ),
                true
        );
    }

    /**
     * 启动 Workflow 运行生命周期，产生初始的 RUN_STARTED 遥测事件。
     *
     * @param context 运行上下文 RuntimeContext
     * @return 启动事件列表 List&lt;RuntimeEvent&gt;
     */
    @Override
    public List<RuntimeEvent> start(RuntimeContext context) {
        return List.of(new RuntimeEvent(
                UUID.randomUUID().toString(),
                context.runId(),
                0,
                RuntimeEventType.RUN_STARTED,
                RunStatus.QUEUED,
                Map.of("mode", RuntimeMode.WORKFLOW.name(), "releaseId", context.releaseId()),
                Instant.now()
        ));
    }

    /**
     * 将固定工作流运行任务提交压入异步 Worker 持久化执行队列。
     *
     * @param context 运行上下文 RuntimeContext
     * @return 执行系统返回的入队结果 Map
     */
    public Map<String, Object> enqueue(RuntimeContext context) {
        if (context == null) {
            throw new IllegalArgumentException("入队固定工作流时，运行上下文 RuntimeContext 不能为空。");
        }

        SecurityUser user = new SecurityUser(
                parseActorId(context.actorId()),
                context.tenantId(),
                context.actorId(),
                DEFAULT_ROLE
        );

        return executionService.enqueue(
                user,
                context.applicationId(),
                context.releaseId(),
                EXECUTION_TYPE,
                context.runId(),
                context.conversationId(),
                null,
                context.input()
        );
    }

    /** 解析操作人数字 ID */
    private Long parseActorId(String actorId) {
        try {
            return Long.valueOf(actorId);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Workflow Runtime 操作者标识 [" + actorId + "] 必须为合法的数字 ID。", exception);
        }
    }
}

