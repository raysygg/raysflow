package com.acme.agentstudio.domain.common;

/**
 * 细粒度 RBAC 鉴权与安全访问控制权限编码枚举（Permission Code）。
 */
public enum PermissionCode {

    /** 查看 Agent 配置与元数据权限 */
    AGENT_READ("agent:read"),

    /** 编辑与修改 Agent 配置权限 */
    AGENT_WRITE("agent:write"),

    /** 发布 Agent Release 权限 */
    AGENT_PUBLISH("agent:publish"),

    /** 查看 Workflow 工作流画布权限 */
    WORKFLOW_READ("workflow:read"),

    /** 编辑与修改 Workflow 工作流画布权限 */
    WORKFLOW_WRITE("workflow:write"),

    /** 发布 Workflow 发布版本权限 */
    WORKFLOW_PUBLISH("workflow:publish"),

    /** 检索与预览知识库文档权限 */
    KNOWLEDGE_READ("knowledge:read"),

    /** 执行与调用外部工具 API 权限 */
    TOOL_INVOKE("tool:invoke"),

    /** 人工审批与确认通过/拒绝权限 */
    APPROVAL_APPROVE("approval:approve"),

    /** 查看运营监控与分析大盘权限 */
    ANALYTICS_READ("analytics:read"),

    /** 查看系统审计日志权限 */
    AUDIT_READ("audit:read");

    private final String code;

    PermissionCode(String code) {
        this.code = code;
    }

    /**
     * 获取权限项对应标准的英文字符串编码。
     *
     * @return 权限编码 string
     */
    public String code() {
        return code;
    }
}

