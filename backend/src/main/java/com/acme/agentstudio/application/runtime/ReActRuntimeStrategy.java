package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.model.RuntimeMode;
import org.springframework.stereotype.Component;

/**
 * ReAct（Reasoning + Acting）自主推理工具调用模式原生策略实现（ReAct Runtime Strategy）。
 * 继承自 AbstractNativeRuntimeStrategy，负责 ReAct 模式下的思考推理（Thought）、
 * 工具动作（Action）与观测回调（Observation）事件边界控制。
 */
@Component
public class ReActRuntimeStrategy extends AbstractNativeRuntimeStrategy {

    /**
     * 构造函数：声明所绑定的运行模式为 RuntimeMode.REACT。
     */
    public ReActRuntimeStrategy() {
        super(RuntimeMode.REACT);
    }
}

