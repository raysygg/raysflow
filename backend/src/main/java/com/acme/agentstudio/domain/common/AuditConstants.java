package com.acme.agentstudio.domain.common;

/**
 * 审计日志领域常量定义类（Audit Constants）。
 * 集中管理风险级别、目标资源类型及审计动作 Event 编码。
 */
public final class AuditConstants {

    /** 普通风险级别代号："P3" */
    public static final String RISK_LEVEL_NORMAL = "P3";

    /** 工作流目标资源类型："WORKFLOW" */
    public static final String TARGET_WORKFLOW = "WORKFLOW";

    /** 创建工作流审计动作："WORKFLOW_CREATE" */
    public static final String ACTION_WORKFLOW_CREATE = "WORKFLOW_CREATE";

    /** 发布工作流审计动作："WORKFLOW_PUBLISH" */
    public static final String ACTION_WORKFLOW_PUBLISH = "WORKFLOW_PUBLISH";

    /** 停用工作流审计动作："WORKFLOW_DISABLE" */
    public static final String ACTION_WORKFLOW_DISABLE = "WORKFLOW_DISABLE";

    /** 私有构造函数，防止工具类被实例化 */
    private AuditConstants() {
    }
}

