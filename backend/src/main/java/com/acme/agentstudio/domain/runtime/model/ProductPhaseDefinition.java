package com.acme.agentstudio.domain.runtime.model;

import java.util.List;

/**
 * 迭代演进产品 MVP 阶段标准定义实体 Record（Product Phase Definition）。
 * 包含产品 MVP 阶段枚举 phase (MvpPhase)、阶段名称 name、能力清单 capabilities、前置依赖 dependencies、
 * 退出与阶段验收条件 exitCriteria 以及主导航菜单控制标志 primaryNavigationVisible。
 *
 * @param phase 产品演进 MVP 阶段（MvpPhase）
 * @param name 阶段中文名称
 * @param capabilities 包含的能力特性列表 List&lt;String&gt;
 * @param dependencies 阶段依赖的前置基础设施或技术模块
 * @param exitCriteria 进入下一阶段的退出与验收指标条件列表
 * @param primaryNavigationVisible 是否在前端全局主导航栏可见
 */
public record ProductPhaseDefinition(
        MvpPhase phase,
        String name,
        List<String> capabilities,
        List<String> dependencies,
        List<String> exitCriteria,
        boolean primaryNavigationVisible
) {
    /** 紧凑构造函数做输入属性校验 */
    public ProductPhaseDefinition {
        if (phase == null || name == null || name.isBlank()) {
            throw new IllegalArgumentException("产品阶段标识和名称不能为空");
        }
        capabilities = (capabilities == null) ? List.of() : List.copyOf(capabilities);
        dependencies = (dependencies == null) ? List.of() : List.copyOf(dependencies);
        exitCriteria = (exitCriteria == null) ? List.of() : List.copyOf(exitCriteria);
    }
}

