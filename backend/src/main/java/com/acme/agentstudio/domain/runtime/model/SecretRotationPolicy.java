package com.acme.agentstudio.domain.runtime.model;

/**
 * 企业安全密钥 API Key/Credential 自动定期轮换与安全凭证作用域策略 Record（Secret Rotation Policy）。
 * 包含强制轮换周期天数 rotationDays、新旧密钥平滑过渡交叠天数 overlapDays、
 * 轮换后是否立即吊销旧密钥 revokeOldImmediately 及是否开启强租户物理隔离 tenantScoped。
 *
 * @param rotationDays 自动定期轮换天数
 * @param overlapDays 新旧密钥兼容双活重叠天数
 * @param revokeOldImmediately 成功生成新 Secret 后是否即刻吊销旧 Secret 凭证
 * @param tenantScoped 密钥是否按租户绑定强隔离（企业生产环境强制为 true）
 */
public record SecretRotationPolicy(
        long rotationDays,
        long overlapDays,
        boolean revokeOldImmediately,
        boolean tenantScoped
) {
    /** 紧凑构造函数做输入属性合规校验 */
    public SecretRotationPolicy {
        if (rotationDays < 1 || overlapDays < 0) {
            throw new IllegalArgumentException("密钥轮换期限无效。");
        }
        if (!tenantScoped) {
            throw new IllegalArgumentException("企业密钥必须按租户隔离。");
        }
    }
}

