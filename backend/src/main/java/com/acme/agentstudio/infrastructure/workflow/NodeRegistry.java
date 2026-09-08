package com.acme.agentstudio.infrastructure.workflow;

import com.acme.agentstudio.application.workflow.NodeExecutionDescriptor;
import com.acme.agentstudio.application.workflow.NodeExecutionHandlerRegistry;
import com.acme.agentstudio.infrastructure.persistence.entity.OrchestrationNodeTypeEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.OrchestrationNodeTypeMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 工作流节点类型动态注册表与元数据加载器组件（Node Registry）。
 * 从数据库 `orchestration_node_type` 表动态加载全量节点描述符（NodeTypeDescriptor），
 * 并整合 NodeExecutionHandlerRegistry 执行器注册表，暴露动态 Schema、配置属性校验与可执行能力探测。
 */
@Component
public class NodeRegistry {

    /** JSON 序列化映射组件 */
    private final ObjectMapper objectMapper;

    /** 节点类型数据库 Mapper */
    private final OrchestrationNodeTypeMapper nodeTypeMapper;

    /** 节点执行器策略注册表 */
    private final NodeExecutionHandlerRegistry handlerRegistry;

    /**
     * 构造函数注入依赖。
     *
     * @param objectMapper Jackson 映射组件
     * @param nodeTypeMapper 节点数据库 Mapper
     * @param handlerRegistry 执行处理器注册表
     */
    public NodeRegistry(
            ObjectMapper objectMapper,
            OrchestrationNodeTypeMapper nodeTypeMapper,
            NodeExecutionHandlerRegistry handlerRegistry
    ) {
        this.objectMapper = objectMapper;
        this.nodeTypeMapper = nodeTypeMapper;
        this.handlerRegistry = handlerRegistry;
    }

    /**
     * 获取全量注册的节点类型描述符集合。
     *
     * @return 节点描述符 Collection
     */
    public Collection<NodeTypeDescriptor> list() {
        return List.copyOf(loadDescriptors().values());
    }

    /**
     * 根据节点类型编码查找描述符，若不存在则抛出 IllegalArgumentException。
     *
     * @param nodeType 节点类型编码
     * @return 节点类型描述符
     */
    public NodeTypeDescriptor require(String nodeType) {
        NodeTypeDescriptor descriptor = find(nodeType);
        if (descriptor == null) {
            throw new IllegalArgumentException("工作流节点类型未注册：" + nodeType);
        }
        return descriptor;
    }

    /**
     * 根据节点类型编码查找描述符（未找到返回 null）。
     *
     * @param nodeType 节点类型编码
     * @return 节点类型描述符
     */
    public NodeTypeDescriptor find(String nodeType) {
        return loadDescriptors().get(nodeType);
    }

    /**
     * 判断指定节点类型是否存在配套的可执行 Handler 处理器。
     *
     * @param nodeType 节点类型编码
     * @return true 表示具备 Java 执行器逻辑
     */
    public boolean hasHandler(String nodeType) {
        return handlerRegistry.supports(nodeType);
    }

    /**
     * 从数据库查询最新的节点元数据并组装描述符 Map。
     */
    private Map<String, NodeTypeDescriptor> loadDescriptors() {
        Map<String, NodeTypeDescriptor> descriptors = new LinkedHashMap<>();
        nodeTypeMapper.selectList(new QueryWrapper<OrchestrationNodeTypeEntity>()
                        .orderByAsc("sort_order")
                        .orderByAsc("node_type"))
                .forEach(entity -> descriptors.put(entity.getNodeType(), toDescriptor(objectMapper, entity)));
        return descriptors;
    }

    /** 将数据库实体转换为前端与校验通用的 NodeTypeDescriptor 实例 */
    private NodeTypeDescriptor toDescriptor(ObjectMapper objectMapper, OrchestrationNodeTypeEntity entity) {
        try {
            boolean hasHandler = handlerRegistry.supports(entity.getNodeType());
            NodeExecutionDescriptor execution = hasHandler ? handlerRegistry.descriptor(entity.getNodeType()) : null;

            String category = (execution == null) ? null : execution.category().name();
            String sideEffect = (execution == null) ? null : execution.sideEffect().name();
            boolean repeatable = (execution != null) && execution.repeatable();
            Set<String> controlSignals = (execution == null)
                    ? Set.of()
                    : execution.controlSignals().stream().map(Enum::name).collect(Collectors.toUnmodifiableSet());

            return new NodeTypeDescriptor(
                    entity.getNodeType(),
                    entity.getVersion(),
                    entity.getLabel(),
                    entity.getDescription(),
                    entity.getSymbol(),
                    readSet(objectMapper, entity.getSupportedGraphTypesJson()),
                    readSet(objectMapper, entity.getCapabilitiesJson()),
                    readSet(objectMapper, entity.getPermissionsJson()),
                    readSet(objectMapper, entity.getRequiredConfigFieldsJson()),
                    readSet(objectMapper, entity.getInputPortsJson()),
                    readSet(objectMapper, entity.getOutputPortsJson()),
                    objectMapper.readTree(entity.getConfigSchemaJson()),
                    objectMapper.readTree(entity.getInputSchemaJson()),
                    objectMapper.readTree(entity.getOutputSchemaJson()),
                    entity.getStatus(),
                    entity.getExecutorKey(),
                    execution != null,
                    category,
                    sideEffect,
                    repeatable,
                    controlSignals
            );
        } catch (Exception ex) {
            throw new IllegalStateException("节点目录配置解析失败：" + entity.getNodeType(), ex);
        }
    }

    /** 读取 JSON 字符串为不可变 String Set */
    private Set<String> readSet(ObjectMapper objectMapper, String json) throws Exception {
        Set<String> values = new HashSet<>();
        objectMapper.readTree(json).forEach(value -> values.add(value.asText()));
        return Set.copyOf(values);
    }
}

