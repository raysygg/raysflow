package com.acme.agentstudio.domain.workflow.model;

/**
 * 跨节点变量引用说明 Record（Variable Reference）。
 *
 * @param sourceNodeId 产生该变量的上游源节点 ID
 * @param outputPath 输出数据 JSONPath 属性路径
 * @param dataType 变量数据类型（如 string, number, json, array 等）
 * @param sensitive 是否为敏感数据（敏感数据会在日志和前端中自动模糊脱敏）
 */
public record VariableReference(
        String sourceNodeId,
        String outputPath,
        String dataType,
        boolean sensitive
) {
}

