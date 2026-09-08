package com.acme.agentstudio.domain.runtime.model;

import java.util.List;

/**
 * 平台环境部署与版本升级预检报告实体 Record（Deployment Preflight Report）。
 * 输出整体预检是否通过 passed、阻断部署问题 blockers、警告列表 warnings 以及版本兼容性对比说明 compatibilitySummary。
 *
 * @param passed 是否通过预检校验
 * @param blockers 阻断部署发布的核心阻塞点提示列表
 * @param warnings 非阻断提示性告警列表
 * @param compatibilitySummary 兼容性概览分析摘要
 */
public record DeploymentPreflightReport(
        boolean passed,
        List<String> blockers,
        List<String> warnings,
        String compatibilitySummary
) {
    /** 紧凑构造函数做防空保护 */
    public DeploymentPreflightReport {
        blockers = (blockers == null) ? List.of() : List.copyOf(blockers);
        warnings = (warnings == null) ? List.of() : List.copyOf(warnings);
        compatibilitySummary = (compatibilitySummary == null) ? "" : compatibilitySummary;
    }
}

