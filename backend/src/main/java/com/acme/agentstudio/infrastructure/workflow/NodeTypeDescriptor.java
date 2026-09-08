package com.acme.agentstudio.infrastructure.workflow;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Set;

/**
 * 工作流画布节点类型元数据描述符 Record（Node Type Descriptor）。
 * 声明节点类型编码 nodeType、版本号、前端渲染 UI 标签、只读与写等权限范围、
 * 必填配置字段、输入/输出端口集合、JSON Schema 以及执行器匹配键 executorKey。
 */
public record NodeTypeDescriptor(
        String nodeType,
        String version,
        String label,
        String description,
        String symbol,
        Set<String> supportedGraphTypes,
        Set<String> capabilities,
        Set<String> permissions,
        Set<String> requiredConfigFields,
        Set<String> inputPorts,
        Set<String> outputPorts,
        JsonNode configSchema,
        JsonNode inputSchema,
        JsonNode outputSchema,
        String status,
        String executorKey,
        boolean executable,
        String executionCategory,
        String sideEffect,
        boolean repeatable,
        Set<String> controlSignals
) {
    /**
     * 判断当前节点类型是否支持特定工作流图类型（例如支持 "*" 通配符或具体 graphType）。
     *
     * @param graphType 拓扑图类型
     * @return true 表示支持
     */
    public boolean supportsGraphType(String graphType) {
        return supportedGraphTypes.contains("*") || supportedGraphTypes.contains(graphType);
    }

    /**
     * 判断当前节点类型是否支持特定的输入端口。
     *
     * @param port 端口名称
     * @return true 表示支持
     */
    public boolean supportsInputPort(String port) {
        return inputPorts.contains("*") || inputPorts.contains(port);
    }

    /**
     * 判断当前节点类型是否支持特定的输出端口。
     *
     * @param port 端口名称
     * @return true 表示支持
     */
    public boolean supportsOutputPort(String port) {
        return outputPorts.contains("*") || outputPorts.contains(port);
    }

    /**
     * 判断节点是否处于激活状态且具备可执行能力。
     *
     * @return true 表示节点可用
     */
    public boolean active() {
        return "ACTIVE".equalsIgnoreCase(status) && executable;
    }
}

