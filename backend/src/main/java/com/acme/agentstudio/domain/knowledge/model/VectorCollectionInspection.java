package com.acme.agentstudio.domain.knowledge.model;

/**
 * 向量数据库 Collection 集合表状态、维度与容量诊断巡检实体 Record（Vector Collection Inspection）。
 * 包含可用状态 available、向量维度 dimension、已有 Point 总数 vectorCount、
 * 稀疏向量索引支持标志 sparseAvailable 及关键词精确索引支持标志 exactIndexAvailable。
 *
 * @param available Collection 是否在线就绪且正常可用
 * @param dimension 当前配置的向量维度
 * @param vectorCount 数据库中已有物理 Point 数量
 * @param sparseAvailable 是否已配置并开启 Sparse 稀疏索引
 * @param exactIndexAvailable 是否已配置并开启 Payload 精确过滤索引
 */
public record VectorCollectionInspection(
        boolean available,
        int dimension,
        long vectorCount,
        boolean sparseAvailable,
        boolean exactIndexAvailable
) {
    /** 紧凑构造函数做极值校验 */
    public VectorCollectionInspection {
        dimension = Math.max(0, dimension);
        vectorCount = Math.max(0L, vectorCount);
    }

    /**
     * 构建 Collection 服务不可用状态实例。
     *
     * @return 标记为不可用的 VectorCollectionInspection 对象
     */
    public static VectorCollectionInspection unavailable() {
        return new VectorCollectionInspection(false, 0, 0L, false, false);
    }
}

