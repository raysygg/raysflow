package com.acme.agentstudio.application.workflow;

/**
 * 工作流节点执行策略 SPI 接口（Node Execution Handler）。
 * 业务节点插件化扩展的核心接口，每种节点类型（LLM、RAG、Condition、Loop、HttpRequest 等）实现此接口并注册到 Spring 容器中。
 * 引擎调度器基于注册表解耦执行，无需增加代码中的 switch/case 节点分支。
 */
public interface NodeExecutionHandler {

    /**
     * 返回节点执行声明描述符（包含节点类型、侧效应及控制信号）。
     *
     * @return 节点执行描述符对象
     */
    NodeExecutionDescriptor descriptor();

    /**
     * 执行具体的节点业务逻辑。
     *
     * @param request 节点执行请求上下文（包含租户 ID、节点 ID、配置 JSON、输入变量等）
     * @return 节点执行结果 Outcome 对象
     */
    NodeExecutionOutcome execute(NodeExecutionRequest request);
}

