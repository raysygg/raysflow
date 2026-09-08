package com.acme.agentstudio.domain.workflow.model;

/**
 * 工作流拓扑图中节点间有向边连线定义 Record（Graph Edge）。
 *
 * @param edgeId 边唯一标识
 * @param sourceNodeId 起始/源节点 ID
 * @param sourcePort 起始/源节点输出端口名称
 * @param targetNodeId 目标节点 ID
 * @param targetPort 目标节点输入端口名称
 */
public record GraphEdge(
        String edgeId,
        String sourceNodeId,
        String sourcePort,
        String targetNodeId,
        String targetPort
) {
}

