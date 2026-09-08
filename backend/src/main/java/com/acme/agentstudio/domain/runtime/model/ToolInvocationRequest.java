package com.acme.agentstudio.domain.runtime.model;

import java.util.Map;
import java.util.Set;

/**
 * ToolInvocation 请求数据传输对象 (DTO)。
 * 封装前端或外部传入的 ToolInvocation 操作参数。
 */
/** 工具执行前的标准请求，先校验权限和安全策略，再交给具体执行器。 */
public record ToolInvocationRequest(
        String runId,
        String toolId,
        String targetUri,
        Set<String> requestedDataScopes,
        Set<String> requestedSecretScopes,
        Map<String, Object> arguments,
        SideEffectDescriptor sideEffect) {
    public ToolInvocationRequest {
        if (runId == null || runId.isBlank() || toolId == null || toolId.isBlank()) {
            throw new IllegalArgumentException("Run 标识和工具标识不能为空");
        }
        if (sideEffect == null) throw new IllegalArgumentException("工具副作用描述不能为空");
        requestedDataScopes = requestedDataScopes == null ? Set.of() : Set.copyOf(requestedDataScopes);
        requestedSecretScopes = requestedSecretScopes == null ? Set.of() : Set.copyOf(requestedSecretScopes);
        arguments = arguments == null ? Map.of() : Map.copyOf(arguments);
    }
}
