package com.acme.agentstudio.domain.workflow.model;

/**
 * 运行时 Run 任务类型枚举（Run Type）。
 * 区分正式生产运行（PRODUCTION）、草稿试运行（DRAFT_TEST）以及节点沙箱调试（NODE_DEBUG），
 * 业务入口严格控制，防止客户端将调试/测试运行伪装为正式生产调用计入计量账本。
 */
public enum RunType {

    /** 正式生产调用（计入生产指标与账本） */
    PRODUCTION(true),

    /** 编排画布草稿试运行（不计入生产消耗） */
    DRAFT_TEST(false),

    /** 节点单点沙箱调试 */
    NODE_DEBUG(false);

    /** 是否计入生产业务指标与账本统计 */
    private final boolean productionStatistics;

    /**
     * 构造函数。
     *
     * @param productionStatistics 是否计入生产统计标志
     */
    RunType(boolean productionStatistics) {
        this.productionStatistics = productionStatistics;
    }

    /**
     * 是否计入生产业务指标与统计。
     *
     * @return true 表示计入生产统计
     */
    public boolean productionStatistics() {
        return productionStatistics;
    }
}

