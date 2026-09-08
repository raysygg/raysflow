package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.model.RuntimeContext;
import com.acme.agentstudio.domain.runtime.model.RuntimeMode;
import com.acme.agentstudio.domain.runtime.model.RuntimeStrategyContracts;

import java.util.List;

/**
 * 运行时策略模式的核心通用接口（Runtime Strategy）。
 * 规定所有 RuntimeMode（CHAT / REACT / PLAN / WORKFLOW / MULTI_AGENT）对应策略适配器的基础生命周期契约。
 * 定义策略模板 template()、能力描述符 descriptor()、核心执行入口 execute() 以及生命周期钩子 start() 与 stop()。
 */
public interface RuntimeStrategy {

    /**
     * 返回当前策略实现的运行模式枚举 RuntimeMode。
     *
     * @return 运行模式 RuntimeMode
     */
    RuntimeMode mode();

    /**
     * 返回当前策略的最小配置模板、输入输出契约和能力证据。
     *
     * @return 策略配置模板 StrategyTemplate
     */
    default RuntimeStrategyContracts.StrategyTemplate template() {
        return RuntimeStrategyContracts.templateFor(mode());
    }

    /**
     * 返回目录、编排和运行时共用的策略能力描述 Descriptor。
     *
     * @return 策略能力描述符 Descriptor
     */
    default RuntimeStrategyContracts.Descriptor descriptor() {
        return new RuntimeStrategyContracts.Descriptor(
                mode(),
                mode().name(),
                "RuntimeContext.input",
                "RuntimeStrategyContracts.Result.output",
                "RuntimeContext.layers",
                List.of(
                        RuntimeStrategyContracts.EventKind.STRATEGY_STARTED,
                        RuntimeStrategyContracts.EventKind.CONTEXT_RESOLVED,
                        RuntimeStrategyContracts.EventKind.STRATEGY_COMPLETED,
                        RuntimeStrategyContracts.EventKind.STRATEGY_FAILED
                ),
                false
        );
    }

    /**
     * 统一策略执行入口。若未覆盖接入具体的底层框架与模型，默认返回包含中文诊断原因的失败结果 Result。
     *
     * @param request 策略请求 Request
     * @return 策略执行结果 Result
     */
    default RuntimeStrategyContracts.Result execute(RuntimeStrategyContracts.Request request) {
        if (request == null) {
            throw new IllegalArgumentException("策略请求 Request 不能为空。");
        }
        RuntimeStrategyContracts.Error error = RuntimeStrategyContracts.Error.of(
                RuntimeStrategyContracts.ErrorCode.STRATEGY_NOT_READY,
                request.requestId(),
                "当前策略模式 [" + mode() + "] 尚未绑定可执行的模型、工具或协作执行器。"
        );
        return RuntimeStrategyContracts.Result.failed(
                request.requestId(),
                request.context().runId(),
                request.strategyNodeId(),
                error
        );
    }

    /**
     * 策略启动生命周期钩子。
     *
     * @param context 运行上下文 RuntimeContext
     * @return 产生的初始化事件列表 List&lt;RuntimeEvent&gt;
     */
    default List<RuntimeEvent> start(RuntimeContext context) {
        return List.of();
    }

    /**
     * 策略停止与清理生命周期钩子。
     *
     * @param context 运行上下文 RuntimeContext
     * @return 产生的清理事件列表 List&lt;RuntimeEvent&gt;
     */
    default List<RuntimeEvent> stop(RuntimeContext context) {
        return List.of();
    }
}

