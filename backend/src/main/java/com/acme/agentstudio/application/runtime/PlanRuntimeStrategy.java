package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.model.RuntimeMode;
import org.springframework.stereotype.Component;

/**
 * 计划分解执行模式原生策略实现（Plan Runtime Strategy）。
 * 继承自 AbstractNativeRuntimeStrategy，负责 Plan 模式下的计划生成（Planning）、
 * 步骤拆解（Step Decomposition）与阶段恢复的事件边界控制。
 */
@Component
public class PlanRuntimeStrategy extends AbstractNativeRuntimeStrategy {

    /**
     * 构造函数：声明所绑定的运行模式为 RuntimeMode.PLAN。
     */
    public PlanRuntimeStrategy() {
        super(RuntimeMode.PLAN);
    }
}

