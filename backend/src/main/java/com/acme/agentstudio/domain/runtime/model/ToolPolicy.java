package com.acme.agentstudio.domain.runtime.model;

import java.util.Set;

/**
 * 单个工具与 API 接口在 Release 发布版本中的最小权限、网络出口与副作用安全治理策略 Record（Tool Policy）。
 * 包含工具 ID toolId、风险等级 riskLevel (ToolRiskLevel)、可读写数据域范围 dataScopes、依赖的机密引用范围 secretScopes、
 * 允许请求的域名白名单 allowedDomains、网络出口网络域 networkEgress、是否要求人工确认 requireConfirmation、
 * 副作用分类 sideEffectClass (SideEffectClass) 及单次运行最大调用上限 maxCallsPerRun。
 *
 * @param toolId 工具唯一 ID
 * @param riskLevel 安全风险等级（ToolRiskLevel）
 * @param dataScopes 允许访问的数据范围标签集合
 * @param secretScopes 允许调用的凭证密钥 Reference 集合
 * @param allowedDomains 允许发起网络调用的域名 Host 白名单集合
 * @param networkEgress 允许网络出站出口域标签
 * @param requireConfirmation 执行前是否触发人工确认弹窗（HIGH / CRITICAL 自动置为 true）
 * @param sideEffectClass 副作用分类等级（SideEffectClass）
 * @param maxCallsPerRun 单次 Run 任务内最多允许调用该工具的次数
 */
public record ToolPolicy(
        String toolId,
        ToolRiskLevel riskLevel,
        Set<String> dataScopes,
        Set<String> secretScopes,
        Set<String> allowedDomains,
        Set<String> networkEgress,
        boolean requireConfirmation,
        SideEffectClass sideEffectClass,
        int maxCallsPerRun
) {
    /** 紧凑构造函数做安全与风险等级强制约束 */
    public ToolPolicy {
        if (toolId == null || toolId.isBlank()) {
            throw new IllegalArgumentException("工具标识不能为空");
        }
        if (riskLevel == null || sideEffectClass == null) {
            throw new IllegalArgumentException("工具风险和副作用等级不能为空");
        }
        if (maxCallsPerRun < 0) {
            throw new IllegalArgumentException("工具调用次数上限不能为负数");
        }
        dataScopes = (dataScopes == null) ? Set.of() : Set.copyOf(dataScopes);
        secretScopes = (secretScopes == null) ? Set.of() : Set.copyOf(secretScopes);
        allowedDomains = (allowedDomains == null) ? Set.of() : Set.copyOf(allowedDomains);
        networkEgress = (networkEgress == null) ? Set.of() : Set.copyOf(networkEgress);
        if (riskLevel == ToolRiskLevel.HIGH || riskLevel == ToolRiskLevel.CRITICAL) {
            requireConfirmation = true;
        }
    }
}

