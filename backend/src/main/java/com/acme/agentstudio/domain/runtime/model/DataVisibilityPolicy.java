package com.acme.agentstudio.domain.runtime.model;

/**
 * 平台 Prompt 提示词、Trace 链路日志、用户记忆与模型输入/输出数据的可见性与数据驻留/合规策略 Record（Data Visibility Policy）。
 *
 * @param dataRegion 数据驻留地理区域编码（如 cn-hangzhou / us-east-1）
 * @param promptVisibleToAdmins 管理员是否可查看全局明文 Prompt 资产
 * @param traceVisibleToAdmins 管理员是否可查看完整 Trace 日志
 * @param memoryConsentRequired 记录长期记忆是否要求用户显式授权 Consent
 * @param encryptionEnabled 是否启用全盘落盘加密与机密加解密
 * @param backupEnabled 是否启用跨可用区双活自动备份
 * @param legalHoldSupported 是否支持诉讼保留 Legal Hold 禁删
 * @param anonymizationSupported 是否支持数据导出脱敏与匿名化
 */
public record DataVisibilityPolicy(
        String dataRegion,
        boolean promptVisibleToAdmins,
        boolean traceVisibleToAdmins,
        boolean memoryConsentRequired,
        boolean encryptionEnabled,
        boolean backupEnabled,
        boolean legalHoldSupported,
        boolean anonymizationSupported
) {
    /** 紧凑构造函数做输入属性合规校验 */
    public DataVisibilityPolicy {
        if (dataRegion == null || dataRegion.isBlank() || !encryptionEnabled || !backupEnabled
                || !legalHoldSupported || !anonymizationSupported) {
            throw new IllegalArgumentException("数据区域不能为空且必须启用加密、备份、法律保留和匿名化。");
        }
    }
}

