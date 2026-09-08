package com.acme.agentstudio.application.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流节点执行处理器注册表（Node Execution Handler Registry）。
 * 在 Spring 容器启动时自动扫描所有 NodeExecutionHandler 实例并进行确定性唯一校验注册。
 * 运行时提供高性效的确定性路由与节点执行调度，杜绝隐晦的硬编码判断逻辑。
 */
@Component
public class NodeExecutionHandlerRegistry {

    /** 日志记录器 */
    private static final Logger log = LoggerFactory.getLogger(NodeExecutionHandlerRegistry.class);

    /** 已注册的节点处理器 Map（Key 为节点类型 code） */
    private final Map<String, NodeExecutionHandler> handlers;

    /**
     * 构造函数自动注入容器中所有的 NodeExecutionHandler Bean 并校验唯一性。
     */
    public NodeExecutionHandlerRegistry(List<NodeExecutionHandler> handlerList) {
        Map<String, NodeExecutionHandler> registered = new LinkedHashMap<>();
        for (NodeExecutionHandler handler : handlerList) {
            String type = handler.descriptor().nodeType();
            if (type == null || type.isBlank()) {
                throw new IllegalStateException("节点处理器 [" + handler.getClass().getName() + "] 必须声明非空的节点类型 nodeType。");
            }
            if (registered.putIfAbsent(type, handler) != null) {
                throw new IllegalStateException("检测到重复注册的节点处理器类型：" + type);
            }
        }
        this.handlers = Map.copyOf(registered);
        log.info("工作流节点执行策略注册表初始化完成，共注册 [{}] 个节点策略。", handlers.size());
    }

    /**
     * 检查注册表中是否包含指定节点类型的执行处理器。
     *
     * @param nodeType 节点类型 Code
     * @return 若存在对应处理器则返回 true
     */
    public boolean supports(String nodeType) {
        return handlers.containsKey(nodeType);
    }

    /**
     * 根据节点类型路由并执行节点逻辑。
     *
     * @param request 节点执行请求
     * @return 节点执行 Outcome
     */
    public NodeExecutionOutcome execute(NodeExecutionRequest request) {
        NodeExecutionHandler handler = handlers.get(request.nodeType());
        if (handler == null) {
            throw new IllegalArgumentException("当前系统未注册节点类型为 [" + request.nodeType() + "] 的执行处理器。");
        }
        return handler.execute(request);
    }

    /**
     * 获取当前注册表中已注册的全部节点处理器快照 Map。
     *
     * @return 处理器 Map
     */
    public Map<String, NodeExecutionHandler> snapshot() {
        return handlers;
    }

    /**
     * 查询指定节点类型的描述符信息。
     *
     * @param nodeType 节点类型 Code
     * @return 节点描述符对象
     */
    public NodeExecutionDescriptor descriptor(String nodeType) {
        NodeExecutionHandler handler = handlers.get(nodeType);
        if (handler == null) {
            throw new IllegalArgumentException("当前系统未注册节点类型为 [" + nodeType + "] 的执行处理器。");
        }
        return handler.descriptor();
    }
}

