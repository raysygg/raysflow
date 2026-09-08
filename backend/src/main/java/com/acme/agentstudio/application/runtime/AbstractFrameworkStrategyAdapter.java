package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.model.GovernedWorkerMessage;
import com.acme.agentstudio.domain.runtime.model.NativeFramework;
import com.acme.agentstudio.domain.runtime.model.RuntimeContext;
import com.acme.agentstudio.domain.runtime.model.RuntimeMode;
import com.acme.agentstudio.domain.runtime.model.RuntimeStrategyContracts;
import com.acme.agentstudio.domain.runtime.model.WorkerMessageType;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * 外部 Agent 框架策略适配器抽象基类（Abstract Framework Strategy Adapter）。
 * 统一处理 Worker 租约获取（Lease Acquisition）、框架描述符（AdapterDescriptor）构建与启动控制消息包装。
 */
public abstract class AbstractFrameworkStrategyAdapter implements FrameworkStrategyAdapter {

    /** 初始消息序号常量 */
    private static final long START_SEQUENCE = 0L;

    /** Worker 标识符前缀 */
    private static final String WORKER_ID_PREFIX = "runtime-worker-";

    /** Payload 属性键：Actor ID */
    private static final String ACTOR_KEY = "actorId";

    /** Payload 属性键：Release ID */
    private static final String RELEASE_KEY = "releaseId";

    /** 受治理 Worker 通信协议句柄 */
    private final GovernedWorkerProtocol workerProtocol;

    /**
     * 构造函数注入受治理 Worker 协议。
     *
     * @param workerProtocol Worker 通信协议实现
     */
    protected AbstractFrameworkStrategyAdapter(GovernedWorkerProtocol workerProtocol) {
        this.workerProtocol = workerProtocol;
    }

    /**
     * 启动框架 Runtime 执行实例，申请 Worker 租约并构造启动控制消息。
     *
     * @param context 运行上下文 RuntimeContext
     * @return 包含租约与操作消息的 GovernedWorkerMessage
     */
    @Override
    public GovernedWorkerMessage start(RuntimeContext context) {
        if (context == null) {
            throw new IllegalArgumentException("框架 Runtime 运行上下文不能为空。");
        }
        String workerId = WORKER_ID_PREFIX + framework().name().toLowerCase();
        GovernedWorkerProtocol.WorkerLease lease = workerProtocol.acquire(workerId, context.runId(), framework());
        return new GovernedWorkerMessage(
                workerId,
                lease.leaseId(),
                context.runId(),
                framework(),
                WorkerMessageType.START,
                START_SEQUENCE,
                Map.of(ACTOR_KEY, context.actorId(), RELEASE_KEY, context.releaseId()),
                null
        );
    }

    /**
     * 获取框架策略适配器的描述符契约信息。
     *
     * @return 框架适配器描述契约对象 AdapterDescriptor
     */
    @Override
    public RuntimeStrategyContracts.AdapterDescriptor descriptor() {
        return new RuntimeStrategyContracts.AdapterDescriptor(
                framework(),
                RuntimeStrategyContracts.DEFAULT_ADAPTER_VERSION,
                RuntimeStrategyContracts.AdapterBoundary.PLATFORM_NATIVE,
                Arrays.stream(RuntimeMode.values())
                        .filter(this::supportsMode)
                        .map(mode -> new RuntimeStrategyContracts.SemanticMapping(mode, semantic(mode)))
                        .toList(),
                "平台内置实现统一协作语义，Worker 只负责受治理的异步执行，不依赖外部框架运行"
        );
    }

    /** 检查是否支持特定运行模式 */
    private boolean supportsMode(RuntimeMode mode) {
        return supportedModes().contains(mode);
    }

    /** 映射运行模式到平台语义契约 */
    private RuntimeStrategyContracts.PlatformSemantic semantic(RuntimeMode mode) {
        return switch (mode) {
            case CHAT -> RuntimeStrategyContracts.PlatformSemantic.CHAT_SESSION;
            case REACT -> RuntimeStrategyContracts.PlatformSemantic.REACT_LOOP;
            case PLAN -> RuntimeStrategyContracts.PlatformSemantic.PLAN_GRAPH;
            case WORKFLOW -> RuntimeStrategyContracts.PlatformSemantic.WORKFLOW_GRAPH;
            case MULTI_AGENT -> RuntimeStrategyContracts.PlatformSemantic.MULTI_AGENT_TOPOLOGY;
        };
    }

    /**
     * 辅助静态工具方法：构建 Set 集合。
     *
     * @param values 变长运行模式参数
     * @return 运行模式 Set 集合
     */
    protected static Set<RuntimeMode> modes(RuntimeMode... values) {
        return Set.of(values);
    }
}

