package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.model.RunStatus;
import com.acme.agentstudio.domain.runtime.model.RuntimeContext;
import com.acme.agentstudio.domain.runtime.model.RuntimeMode;
import com.acme.agentstudio.domain.runtime.model.RuntimeStrategyContracts;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 平台原生运行策略抽象基类（Abstract Native Runtime Strategy）。
 * 统一管理特定 RuntimeMode 模式的元数据描述符（Descriptor）、运行启动事件（RUN_STARTED）通知与基础模式暴露。
 */
public abstract class AbstractNativeRuntimeStrategy implements RuntimeStrategy {

    /** 初始事件序号 */
    private static final long FIRST_EVENT_SEQUENCE = 0L;

    /** 绑定的运行模式 Enum */
    private final RuntimeMode runtimeMode;

    /**
     * 构造函数注入绑定的运行模式。
     *
     * @param runtimeMode 运行模式 RuntimeMode
     */
    protected AbstractNativeRuntimeStrategy(RuntimeMode runtimeMode) {
        this.runtimeMode = runtimeMode;
    }

    /**
     * 获取当前策略绑定的运行模式。
     *
     * @return 运行模式 Enum
     */
    @Override
    public RuntimeMode mode() {
        return runtimeMode;
    }

    /**
     * 获取当前原生运行策略的契约描述符信息。
     *
     * @return 策略描述符对象 Descriptor
     */
    @Override
    public RuntimeStrategyContracts.Descriptor descriptor() {
        return new RuntimeStrategyContracts.Descriptor(
                runtimeMode,
                runtimeMode.name(),
                "RuntimeContext.input",
                "RuntimeStrategyContracts.Result.output",
                "RuntimeContext.layers（输入、短期记忆、长期记忆、检索结果和流程变量）",
                List.of(
                        RuntimeStrategyContracts.EventKind.STRATEGY_STARTED,
                        RuntimeStrategyContracts.EventKind.CONTEXT_RESOLVED,
                        RuntimeStrategyContracts.EventKind.STEP_STARTED,
                        RuntimeStrategyContracts.EventKind.STRATEGY_COMPLETED,
                        RuntimeStrategyContracts.EventKind.STRATEGY_FAILED
                ),
                runtimeMode == RuntimeMode.CHAT
        );
    }

    /**
     * 启动当前模式的运行流程，发出 RUN_STARTED 初始阶段事件。
     *
     * @param context 运行上下文 RuntimeContext
     * @return 包含初始启动事件的列表 List&lt;RuntimeEvent&gt;
     */
    @Override
    public List<RuntimeEvent> start(RuntimeContext context) {
        if (context == null) {
            throw new IllegalArgumentException("运行上下文 RuntimeContext 不能为空。");
        }
        return List.of(new RuntimeEvent(
                UUID.randomUUID().toString(),
                context.runId(),
                FIRST_EVENT_SEQUENCE,
                RuntimeEventType.RUN_STARTED,
                RunStatus.RUNNING,
                Map.of("mode", runtimeMode.name()),
                Instant.now()
        ));
    }
}

