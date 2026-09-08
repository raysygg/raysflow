package com.acme.agentstudio.domain.common;

/**
 * 模板引擎动态替换的系统保留 Prompt 占位符 Key 常量集中定义类（Prompt Placeholders）。
 */
public final class PromptPlaceholders {

    /** 用户最新对话输入消息占位符 Key："{{variables.user_message}}" */
    public static final String USER_MESSAGE = "{{variables.user_message}}";

    /** RAG 检索知识库上下文占位符 Key："{{variables.rag_context}}" */
    public static final String RAG_CONTEXT = "{{variables.rag_context}}";

    /** 私有构造函数，防止工具类被实例化 */
    private PromptPlaceholders() {
    }
}

