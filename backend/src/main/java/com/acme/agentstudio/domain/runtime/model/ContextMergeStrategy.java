package com.acme.agentstudio.domain.runtime.model;

/**
 * 工作流并行分支（Parallel Nodes）回写共享上下文时的合并碰撞冲突策略枚举（Context Merge Strategy）。
 */
public enum ContextMergeStrategy {

    /** 存在同名变量冲突时拒绝合并并抛出异常 */
    REJECT_CONFLICT,

    /** 优先保留最先执行完成的分支返回变量 */
    FIRST_COMPLETED,

    /** 优先覆盖使用拓扑图中最后声明的分支变量 */
    LAST_DECLARED
}

