package com.acme.agentstudio.application.saas;

/**
 * 租户数据治理存储适配器（Data Governance Store Adapter） SPI 接口。
 * 负责定义跨 MySQL、S3/OSS 对象存储及 Qdrant 向量数据库执行数据清理预检（preview）与合规删除（delete）的抽象边界，证据链仅包含审计校验和与安全摘要。
 */
public interface DataGovernanceStoreAdapter {

    /**
     * 获取当前存储适配器的类型标识。
     *
     * @return 存储类型（如 MYSQL_RELATIONAL / OBJECT_STORAGE / VECTOR_DB）
     */
    String storeType();

    /**
     * 预检指定治理范围下的待处理数据规模与受控阻塞状态。
     *
     * @param tenantId 租户 ID
     * @param scopeJson 治理范围配置 JSON
     * @return 存储数据预检结果
     */
    StorePreview preview(Long tenantId, String scopeJson);

    /**
     * 执行合规数据物理/逻辑物理删除。
     *
     * @param tenantId 租户 ID
     * @param scopeJson 治理范围配置 JSON
     * @param idempotencyKey 幂等校验 Key
     * @return 存储数据删除结果
     */
    StoreExecution delete(Long tenantId, String scopeJson, String idempotencyKey);

    /**
     * 存储数据治理预检结果 Record。
     *
     * @param objectCount 受影响的数据对象数量
     * @param safeSummary 预检摘要描述
     * @param blocked 是否包含被合规合规保留策略阻塞的对象
     */
    record StorePreview(
            long objectCount,
            String safeSummary,
            boolean blocked
    ) { }

    /**
     * 存储数据治理删除执行结果 Record。
     *
     * @param success 是否执行成功
     * @param objectCount 实际物理清理的数据对象数量
     * @param checksum 数据删除凭据 SHA-256 校验和
     * @param safeSummary 安全审计摘要
     */
    record StoreExecution(
            boolean success,
            long objectCount,
            String checksum,
            String safeSummary
    ) { }
}

