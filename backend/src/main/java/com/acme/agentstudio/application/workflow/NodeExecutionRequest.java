package com.acme.agentstudio.application.workflow;

import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.workflow.model.WorkflowDependencySnapshot;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * 节点执行请求上下文 Record（Node Execution Request）。
 * 封装由工作流引擎准备并传递给特定 NodeExecutionHandler 的标准输入参数。
 *
 * @param user 当前提交运行的安全用户上下文
 * @param nodeId 节点 ID
 * @param nodeType 节点类型 Code
 * @param config 节点配置 JSON 节点
 * @param inputs 节点解析后的输入变量封装对象
 * @param visitCount 节点被重入访问的次数（循环节点有用）
 * @param dependencies 当前发布版本的关联冻结依赖快照
 * @param modelDeltaListener Token 流式响应监听器
 */
public record NodeExecutionRequest(
        SecurityUser user,
        String nodeId,
        String nodeType,
        JsonNode config,
        NodeInputValues inputs,
        int visitCount,
        WorkflowDependencySnapshot dependencies,
        LlmNodeExecutor.ModelDeltaListener modelDeltaListener
) {
    /** 构造函数安全防空初始化 */
    public NodeExecutionRequest {
        inputs = inputs == null ? new NodeInputValues(null) : inputs;
        dependencies = dependencies == null ? WorkflowDependencySnapshot.empty() : dependencies;
        modelDeltaListener = modelDeltaListener == null ? LlmNodeExecutor.ModelDeltaListener.NOOP : modelDeltaListener;
    }

    /**
     * 获取请求关联的租户 ID。
     *
     * @return 租户 ID，无法获取时返回 null
     */
    public Long tenantId() {
        return user == null ? null : user.getTenantId();
    }
}

