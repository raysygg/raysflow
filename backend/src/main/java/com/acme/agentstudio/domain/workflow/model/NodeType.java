package com.acme.agentstudio.domain.workflow.model;

/**
 * 工作流节点类型底层协议编码枚举（Node Type）。
 * 约定工作流引擎与图节点执行器认可的标准编码，展示文案、属性 Schema 及权限元数据由数据库 `orchestration_node_type` 目录统一维护。
 */
public enum NodeType {

    /** 用户输入节点 */
    USER_INPUT("USER_INPUT"),

    /** 流程开始节点 */
    START("START"),

    /** 流程结束节点 */
    END("END"),

    /** 直接回复文本节点 */
    DIRECT_REPLY("DIRECT_REPLY"),

    /** LLM 大语言模型节点 */
    LLM("LLM"),

    /** RAG 知识库检索与生成节点 */
    RAG("RAG"),

    /** 子 Agent 代理节点 */
    AGENT("AGENT"),

    /** 问题意图分类节点 */
    QUESTION_CLASSIFIER("QUESTION_CLASSIFIER"),

    /** 条件分支路由节点 */
    ROUTER("ROUTER"),

    /** 参数提取节点 */
    PARAMETER_EXTRACTOR("PARAMETER_EXTRACTOR"),

    /** 条件判断节点 */
    CONDITION("CONDITION"),

    /** 迭代节点 */
    ITERATION("ITERATION"),

    /** 并行分支节点 */
    PARALLEL("PARALLEL"),

    /** 并行汇聚 Join 节点 */
    JOIN("JOIN"),

    /** Loop 循环控制节点 */
    LOOP("LOOP"),

    /** 自定义代码运行节点（Python/Groovy/JS） */
    CODE("CODE"),

    /** 模板转换节点 */
    TEMPLATE_TRANSFORM("TEMPLATE_TRANSFORM"),

    /** 变量聚合节点 */
    VARIABLE_AGGREGATOR("VARIABLE_AGGREGATOR"),

    /** 文档解析提取节点 */
    DOCUMENT_EXTRACTOR("DOCUMENT_EXTRACTOR"),

    /** 变量赋值节点 */
    VARIABLE_ASSIGNMENT("VARIABLE_ASSIGNMENT"),

    /** 列表数据操作节点 */
    LIST_OPERATOR("LIST_OPERATOR"),

    /** HTTP 外部接口请求节点 */
    HTTP_REQUEST("HTTP_REQUEST"),

    /** OpenAPI 描述工具调用节点 */
    OPENAPI("OPENAPI"),

    /** Model Context Protocol (MCP) 协议工具节点 */
    MCP("MCP"),

    /** Webhook 事件回调节点 */
    WEBHOOK("WEBHOOK"),

    /** 内部 API 调用节点 */
    INTERNAL_API("INTERNAL_API"),

    /** Web 网络爬虫抓取节点 */
    WEB_CRAWLER("WEB_CRAWLER"),

    /** 通用转换节点 */
    TRANSFORM("TRANSFORM"),

    /** 人工审批交互节点 */
    HUMAN("HUMAN"),

    /** 上下文构建节点 */
    CONTEXT_BUILDER("CONTEXT_BUILDER"),

    /** Prompt 模板渲染节点 */
    PROMPT_TEMPLATE("PROMPT_TEMPLATE"),

    /** 会话记忆存储节点 */
    SESSION_MEMORY("SESSION_MEMORY"),

    /** ReAct 推理 Agent 节点 */
    REASONING_AGENT("REACT_AGENT"),

    /** Plan-Execute 规划 Agent 节点 */
    PLANNING_AGENT("PLAN_AGENT"),

    /** Agent 团队协作节点 */
    AGENT_TEAM("AGENT_TEAM"),

    /** 拓扑图规划编排节点 */
    GRAPH_PLANNING("GRAPH_ORCHESTRATOR"),

    /** 多角色 Agent 节点 */
    MULTI_ROLE_AGENT("MULTI_AGENT");

    /** 节点协议编码 */
    private final String code;

    /**
     * 构造函数。
     *
     * @param code 协议编码字符串
     */
    NodeType(String code) {
        this.code = code;
    }

    /**
     * 获取节点协议编码。
     *
     * @return 编码字符串
     */
    public String code() {
        return code;
    }
}

