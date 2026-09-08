package com.acme.agentstudio.application.workflow;

/**
 * 工作流运行时规范变量名称常量类（Workflow Variable Names）。
 * 集中管理系统预留的 input.xxx、variables.xxx、context.xxx 等规范变量键名，禁止节点分散硬编码魔法字符串。
 */
public final class WorkflowVariableNames {

    /** 入口请求主对象变量 */
    public static final String INPUT_REQUEST = "input.request";

    /** 入口 input 字符串变量 */
    public static final String INPUT_INPUT = "input.input";

    /** 入口 query 搜索变量 */
    public static final String INPUT_QUERY = "input.query";

    /** 入口用户消息变量 */
    public static final String INPUT_USER_MESSAGE = "input.user_message";

    /** 全局 input 变量 */
    public static final String VARIABLE_INPUT = "variables.input";

    /** 全局 query 变量 */
    public static final String VARIABLE_QUERY = "variables.query";

    /** 全局用户消息变量 */
    public static final String VARIABLE_USER_MESSAGE = "variables.user_message";

    /** RAG 上下文渲染变量 */
    public static final String VARIABLE_RAG_CONTEXT = "variables.rag_context";

    /** RAG 检索明细结果变量 */
    public static final String VARIABLE_RAG_RESULTS = "variables.rag_results";

    /** 上下文会话历史变量 */
    public static final String CONTEXT_CONVERSATION_HISTORY = "context.conversationHistory";

    /** 上下文短期记忆变量 */
    public static final String CONTEXT_SHORT_TERM_MEMORY = "context.shortTermMemory";

    /** 上下文长期记忆变量 */
    public static final String CONTEXT_LONG_TERM_MEMORY = "context.longTermMemory";

    /** 上下文检索知识变量 */
    public static final String CONTEXT_RETRIEVED_KNOWLEDGE = "context.retrievedKnowledge";

    /**
     * 私有构造函数，防止实例化常量类。
     */
    private WorkflowVariableNames() {
    }
}

