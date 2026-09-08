package com.acme.agentstudio.application.runtime;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 大模型供应商调用熔断保护服务（Model Provider Circuit Breaker）。
 * 针对上游 LLM/Embedding/Reranker 模型供应商的调用故障率（如连续超时、503 服务不可用）提供熔断保护：
 * 连续失败达到阈值（如 3 次）时自动熔断开启（OPEN 状态 30 秒），在此期间直接拦截请求或引导降级备用模型路由，避免死等超时。
 */
@Service
public class ModelProviderCircuitBreaker {

    /** 触发熔断的默认连续失败次数阈值 */
    private static final int DEFAULT_FAILURE_THRESHOLD = 3;

    /** 熔断器开启（OPEN）状态持续保持时长（秒） */
    private static final long DEFAULT_OPEN_SECONDS = 30L;

    /** 供应商熔断器状态存储 Map */
    private final Map<String, Circuit> circuits = new ConcurrentHashMap<>();

    /**
     * 检查指定模型供应商当前是否允许发起网络请求。
     *
     * @param provider 供应商名称或标识 ID
     * @return true 表示正常允许调用，false 表示处于熔断拦截中
     */
    public boolean allow(String provider) {
        Circuit circuit = circuits.get(provider);
        if (circuit == null) {
            return true;
        }
        if (circuit.state() != CircuitState.OPEN) {
            return true;
        }
        return Instant.now().isAfter(circuit.openUntil());
    }

    /**
     * 记录一次调用成功，自动重置清除该供应商的连续失败计数与熔断状态。
     *
     * @param provider 供应商标识
     */
    public void recordSuccess(String provider) {
        circuits.remove(provider);
    }

    /**
     * 记录一次模型调用异常失败，连续失败达到阈值时触发熔断。
     *
     * @param provider 供应商标识
     */
    public void recordFailure(String provider) {
        Circuit current = circuits.getOrDefault(provider, new Circuit(CircuitState.CLOSED, 0, Instant.MIN));
        int failures = current.failures() + 1;

        Circuit updated;
        if (failures >= DEFAULT_FAILURE_THRESHOLD) {
            updated = new Circuit(CircuitState.OPEN, failures, Instant.now().plusSeconds(DEFAULT_OPEN_SECONDS));
        } else {
            updated = new Circuit(CircuitState.CLOSED, failures, Instant.MIN);
        }
        circuits.put(provider, updated);
    }

    /** 熔断器开关状态枚举 */
    private enum CircuitState {
        /** 闭合正常状态 */
        CLOSED,
        /** 开启熔断状态 */
        OPEN
    }

    /** 熔断器状态描述 Record */
    private record Circuit(CircuitState state, int failures, Instant openUntil) {
    }
}

