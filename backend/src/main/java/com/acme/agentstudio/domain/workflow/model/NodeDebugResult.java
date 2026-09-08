package com.acme.agentstudio.domain.workflow.model;

/**
 * 工作流单个节点在线调试与沙箱测试结果契约 Record（Node Debug Result）。
 *
 * @param executionId 单次调试关联的执行 ID
 * @param runType 运行类型（RunType）
 * @param status 调试运行状态（SUCCEEDED / FAILED 等）
 * @param nodeType 调度的节点类型编码
 * @param output 节点试运行输出数据结果 Object
 * @param errorMessage 调试异常时的中文错误详细信息
 */
public record NodeDebugResult(
        String executionId,
        RunType runType,
        String status,
        String nodeType,
        Object output,
        String errorMessage
) {
}

