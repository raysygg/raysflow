package com.acme.agentstudio.application.saas;

/**
 * 租户合规数据打包导出适配器（Governance Export Adapter） SPI 接口。
 * 负责将指定租户的离职/注销数据归档打包导出为带有加密与校验和的保护文件，API 端点仅返回受控凭证与 Hash。
 */
public interface GovernanceExportAdapter {

    /**
     * 根据导出治理请求异步生成数据归档压缩包。
     *
     * @param tenantId 租户 ID
     * @param requestId 导出治理工单 ID
     * @param scopeJson 数据导出范围 JSON 配置
     * @return 数据导出结果对象
     */
    ExportResult generate(Long tenantId, Long requestId, String scopeJson);

    /**
     * 数据归档导出结果 Record。
     *
     * @param success 是否导出成功
     * @param protectedReference 受保护归档文件的安全下载凭证引用
     * @param checksum 归档包文件 SHA-256 完整性校验和
     * @param objectCount 归档包含的数据记录总行数/文件数
     * @param safeSummary 导出的审计安全摘要
     */
    record ExportResult(
            boolean success,
            String protectedReference,
            String checksum,
            long objectCount,
            String safeSummary
    ) { }
}

