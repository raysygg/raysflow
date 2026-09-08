package com.acme.agentstudio.application.task;

/**
 * 平台持久化后台异步任务（Persistent Async Task）类型常量定义。
 * 用于生产者（Task Queue）入队与消费者（Worker）分配派发，避免散落硬编码魔法字符串。
 */
public final class TaskType {

    /** 任务类型：单个知识库文档向量重索引 */
    public static final String KNOWLEDGE_REINDEX = "KNOWLEDGE_REINDEX";

    /** 任务类型：知识库文档批量向量重索引 */
    public static final String KNOWLEDGE_REINDEX_BATCH = "KNOWLEDGE_REINDEX_BATCH";

    /** 任务类型：生产线上工作流与 Agent 编排异步调度执行 */
    public static final String ORCHESTRATION_EXECUTION = "ORCHESTRATION_EXECUTION";

    /** 任务类型：画布草稿版本调试与试运行 */
    public static final String ORCHESTRATION_DRAFT_TEST = "ORCHESTRATION_DRAFT_TEST";

    /** 任务类型：中断/审批后的工作流恢复执行 */
    public static final String ORCHESTRATION_RESUME = "ORCHESTRATION_RESUME";

    /** 任务类型：失败工作流与 Runtime 节点故障修复恢复 */
    public static final String ORCHESTRATION_RECOVERY = "ORCHESTRATION_RECOVERY";

    /** 任务类型：应用候选版本自动化评测套件运行 */
    public static final String APPLICATION_EVALUATION = "APPLICATION_EVALUATION";

    /**
     * 私有构造函数，防止工具类实例化。
     */
    private TaskType() {
    }
}

