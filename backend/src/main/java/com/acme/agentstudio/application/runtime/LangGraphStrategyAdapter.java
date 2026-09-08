package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.model.NativeFramework;
import com.acme.agentstudio.domain.runtime.model.RuntimeMode;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * LangGraph 框架策略适配器（LangGraph Strategy Adapter）。
 * 负责将基于图计算与状态转换的 LangGraph 抽象转化为平台统一的图状态、断点恢复与检查点 Savepoint 协议。
 */
@Component
public class LangGraphStrategyAdapter extends AbstractFrameworkStrategyAdapter {

    /**
     * 构造函数注入 Worker 治理协议工具。
     *
     * @param workerProtocol Worker 治理协议工具类 GovernedWorkerProtocol
     */
    public LangGraphStrategyAdapter(GovernedWorkerProtocol workerProtocol) {
        super(workerProtocol);
    }

    /**
     * 获取对应的原生框架 NativeFramework.LANGGRAPH。
     *
     * @return 原生框架枚举
     */
    @Override
    public NativeFramework framework() {
        return NativeFramework.LANGGRAPH;
    }

    /**
     * 获取 LangGraph 支持的运行模式（WORKFLOW、REACT、PLAN）。
     *
     * @return 运行模式集合 Set&lt;RuntimeMode&gt;
     */
    @Override
    public Set<RuntimeMode> supportedModes() {
        return modes(RuntimeMode.WORKFLOW, RuntimeMode.REACT, RuntimeMode.PLAN);
    }
}

