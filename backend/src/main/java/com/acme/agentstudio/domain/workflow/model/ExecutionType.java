package com.acme.agentstudio.domain.workflow.model;

import java.util.Set;

/**
 * 应用统一执行类型常量类（Execution Type）。
 * 定义应用工作流在引擎内部的执行类型，区分生产主流程与测试流程，消除入口差异对产品工作流类型的影响。
 */
public final class ExecutionType {

    /** 应用主工作流生产执行类型常量 */
    public static final String APPLICATION_WORKFLOW = "APPLICATION_WORKFLOW";

    /** 全部受支持的工作流执行类型集合 */
    public static final Set<String> ALL = Set.of(APPLICATION_WORKFLOW);

    /** 私有构造函数，防止工具类被实例化 */
    private ExecutionType() {
    }
}

