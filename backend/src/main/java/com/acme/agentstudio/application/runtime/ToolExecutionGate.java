package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.model.ToolDryRunResult;
import com.acme.agentstudio.domain.runtime.model.ToolExecutionMode;
import com.acme.agentstudio.domain.runtime.model.ToolInvocationRequest;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 运行时 Agent 工具试运行（Dry Run）与隔离闸门组件（Tool Execution Gate）。
 * 当工具调用模式为 DRY_RUN 模拟调试模式时，截断真实的网络请求与外部写操作，
 * 返回安全不产生外部副作用的模拟执行结果 ToolDryRunResult，供前端或测试调试查看。
 */
@Component
public class ToolExecutionGate {

    /**
     * 对指定的工具调用请求 ToolInvocationRequest 执行 Dry Run 模拟调试。
     *
     * @param request 工具调用请求对象 ToolInvocationRequest
     * @return 模拟工具执行结果 ToolDryRunResult
     */
    public ToolDryRunResult dryRun(ToolInvocationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("执行工具 Dry Run 试运行模拟时，请求对象 ToolInvocationRequest 不能为空。");
        }

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("status", "SIMULATED");
        output.put("message", "已完成工具模拟试运行，未向外部发起网络请求或产生副作用。");
        output.put("actionId", request.sideEffect().actionId());
        output.put("arguments", request.arguments());

        return new ToolDryRunResult(
                request.toolId(),
                ToolExecutionMode.DRY_RUN,
                true,
                output
        );
    }
}

