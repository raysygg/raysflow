package com.acme.agentstudio.domain.runtime.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 单次 Agent/Workflow 运行的任务上下文分层包含容器类（Runtime Context Layers）。
 * 原始数据层只读，Prompt 变量视图 promptVariables 允许执行节点追加写输出，并在整个运行期间复用同一份数据。
 * 层级优先级合并顺序从低到高为：长期记忆、短期记忆、检索结果、流程变量、会话输入。
 */
public final class RuntimeContextLayers {

    /** 检索知识库在变量表中的内部 key 标示 */
    private static final String RETRIEVED_KNOWLEDGE_VARIABLE = "retrievedKnowledge";

    /** RAG 检索上下文在变量表中的缩写 key 标示 */
    private static final String RAG_CONTEXT_VARIABLE = "rag_context";

    /** 原始只读用户输入层 Map */
    private final Map<String, Object> input;

    /** 原始只读流程变量层 Map */
    private final Map<String, Object> variables;

    /** 原始只读短期对话记忆层 Map */
    private final Map<String, Object> shortTermMemory;

    /** 原始只读长期记忆事实层 Map */
    private final Map<String, Object> longTermMemory;

    /** 原始只读 RAG 检索知识召回层 Map */
    private final Map<String, Object> retrievedKnowledge;

    /** 运行期可读可追加的合并 Prompt 动态变量视图 Map */
    private final Map<String, Object> promptVariables;

    /**
     * 构造函数并依照优先级叠加合并上下文分层变量。
     *
     * @param input 用户输入 Map
     * @param variables 流程节点变量 Map
     * @param shortTermMemory 短期对话记忆 Map
     * @param longTermMemory 长期记忆事实 Map
     * @param retrievedKnowledge RAG 检索召回知识 Map
     */
    public RuntimeContextLayers(
            Map<String, Object> input,
            Map<String, Object> variables,
            Map<String, Object> shortTermMemory,
            Map<String, Object> longTermMemory,
            Map<String, Object> retrievedKnowledge
    ) {
        this.input = immutable(input);
        this.variables = immutable(variables);
        this.shortTermMemory = immutable(shortTermMemory);
        this.longTermMemory = immutable(longTermMemory);
        this.retrievedKnowledge = immutable(retrievedKnowledge);
        this.promptVariables = new LinkedHashMap<>();
        this.promptVariables.putAll(this.longTermMemory);
        this.promptVariables.putAll(this.shortTermMemory);
        if (!this.retrievedKnowledge.isEmpty()) {
            this.promptVariables.put(RETRIEVED_KNOWLEDGE_VARIABLE, this.retrievedKnowledge);
            this.promptVariables.put(RAG_CONTEXT_VARIABLE, this.retrievedKnowledge);
        }
        this.promptVariables.putAll(this.variables);
        this.promptVariables.putAll(this.input);
    }

    /**
     * 获取原始只读输入参数字典 Map。
     *
     * @return 不可变输入 Map
     */
    public Map<String, Object> input() {
        return input;
    }

    /**
     * 获取原始只读流程节点变量字典 Map。
     *
     * @return 不可变变量 Map
     */
    public Map<String, Object> variables() {
        return variables;
    }

    /**
     * 获取原始只读短期对话记忆字典 Map。
     *
     * @return 不可变短期记忆 Map
     */
    public Map<String, Object> shortTermMemory() {
        return shortTermMemory;
    }

    /**
     * 获取原始只读长期记忆事实字典 Map。
     *
     * @return 不可变长期记忆 Map
     */
    public Map<String, Object> longTermMemory() {
        return longTermMemory;
    }

    /**
     * 获取原始只读 RAG 检索知识召回字典 Map。
     *
     * @return 不可变检索知识 Map
     */
    public Map<String, Object> retrievedKnowledge() {
        return retrievedKnowledge;
    }

    /**
     * 返回运行期共享变量表 Map，节点输出写入后会被后续节点和 Prompt 继续读取。
     *
     * @return 运行期可变共享变量 Map 视图
     */
    public Map<String, Object> promptVariables() {
        return promptVariables;
    }

    private static Map<String, Object> immutable(Map<String, Object> value) {
        return (value == null || value.isEmpty()) ? Map.of() : Map.copyOf(value);
    }
}

