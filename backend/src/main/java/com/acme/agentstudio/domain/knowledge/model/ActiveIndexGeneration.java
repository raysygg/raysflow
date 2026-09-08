package com.acme.agentstudio.domain.knowledge.model;

/** 当前模型空间对应的 Qdrant 集合和索引版本。 */
public record ActiveIndexGeneration(Long id, Long tenantId, Long profileId, String collectionName) {
}

