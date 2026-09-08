package com.acme.agentstudio.domain.common;

/**
 * 单次 Agent / Workflow 执行全流程步骤阶段名称常量定义类（Execution Step）。
 */
public final class ExecutionStep {

    /** 加载 Agent/Release 配置："LOAD_AGENT" */
    public static final String LOAD_AGENT = "LOAD_AGENT";

    /** 执行 RAG 知识检索与重排："RETRIEVE_RAG" */
    public static final String RETRIEVE_RAG = "RETRIEVE_RAG";

    /** 渲染填充 Prompt 模板："RENDER_PROMPT" */
    public static final String RENDER_PROMPT = "RENDER_PROMPT";

    /** 调用大模型生成回答："CALL_MODEL" */
    public static final String CALL_MODEL = "CALL_MODEL";

    /** 持久化运行 Trace 审计节点："PERSIST_TRACE" */
    public static final String PERSIST_TRACE = "PERSIST_TRACE";

    /** 私有构造函数，防止工具类被实例化 */
    private ExecutionStep() {
    }
}

