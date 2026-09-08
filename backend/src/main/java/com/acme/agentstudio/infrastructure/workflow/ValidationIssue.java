package com.acme.agentstudio.infrastructure.workflow;

/**
 * 工作流画布拓扑图与节点契约校验异常问题条目 Record（Validation Issue）。
 *
 * @param level 问题严重等级（BLOCKING 阻断性错误 / WARNING 告警提示）
 * @param code 错误识别码
 * @param nodeId 关联的节点 ID
 * @param fieldPath 触发校验问题的字段 JSONPath 路径
 * @param message 中文错误描述
 * @param suggestion 建议修复方向或操作指引
 */
public record ValidationIssue(
        String level,
        String code,
        String nodeId,
        String fieldPath,
        String message,
        String suggestion
) {
    /**
     * 静态工厂方法：构建 BLOCKING 阻断性错误校验条目。
     *
     * @param code 错误编码
     * @param nodeId 关联节点 ID
     * @param fieldPath 字段路径
     * @param message 错误描述文案
     * @param suggestion 修复建议
     * @return ValidationIssue 实例
     */
    public static ValidationIssue error(String code, String nodeId, String fieldPath, String message, String suggestion) {
        return new ValidationIssue("BLOCKING", code, nodeId, fieldPath, message, suggestion);
    }
}

