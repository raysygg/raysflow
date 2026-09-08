package com.acme.agentstudio.domain.runtime.model;

/**
 * 生产级别向量数据库（如 Qdrant / Milvus）存储与隔离策略契约 Record（Vector Storage Contract）。
 * 明确向量存储供应商 provider、强制租户隔离 tenantIsolation、多租户共享 cluster shared、
 * 备份使能 backupEnabled、监控告警使能 monitoringEnabled 与物理部署区域 region。
 *
 * @param provider 向量数据库供应商名称（如 Qdrant）
 * @param tenantIsolation 是否在 Collection/Payload 维度开启租户强隔离
 * @param shared 是否为集群多租户共享存储
 * @param backupEnabled 是否开启向量快照与增量备份
 * @param monitoringEnabled 是否接入 Prometheus/Grafana 存储监控
 * @param region 向量数据库所在的物理部署区域
 */
public record VectorStorageContract(
        String provider,
        boolean tenantIsolation,
        boolean shared,
        boolean backupEnabled,
        boolean monitoringEnabled,
        String region
) {
    /** 紧凑构造函数做生产合规强校验 */
    public VectorStorageContract {
        if (provider == null || provider.isBlank() || region == null || region.isBlank()) {
            throw new IllegalArgumentException("向量存储供应商和区域不能为空");
        }
        if (!tenantIsolation || !shared || !backupEnabled || !monitoringEnabled) {
            throw new IllegalArgumentException("生产向量存储必须支持租户隔离、共享、备份和监控");
        }
    }
}

