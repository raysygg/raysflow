package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.model.RuntimeMode;
import org.springframework.stereotype.Component;

/**
 * 会话交互模式系统原生运行策略（Chat Runtime Strategy）。
 * 负责统一处理 CHAT 模式下的会话上下文、交互步骤与 Run 生命周期。
 */
@Component
public class ChatRuntimeStrategy extends AbstractNativeRuntimeStrategy {

    /**
     * 默认构造函数，向基类传递 RuntimeMode.CHAT 标识。
     */
    public ChatRuntimeStrategy() {
        super(RuntimeMode.CHAT);
    }
}

