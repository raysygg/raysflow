package com.acme.agentstudio.domain.knowledge.model;

/**
 * 外部 Reranker 模型或分词服务故障时的降级熔断策略枚举（Retrieval Degrade Policy）。
 */
public enum RetrievalDegradePolicy {

    /** 旧配置名兼容，语义等同于允许回退降级到多路 RRF 融合排序 */
    VECTOR_ONLY,

    /** 旧配置名兼容，语义等同于熔断关闭失败 */
    FAIL,

    /** 降级放行（Reranker 故障时自动退回到基础向量/词法融合召回） */
    FAIL_OPEN,

    /** 降级阻断（Reranker 故障时直接报错拒答） */
    FAIL_CLOSED;

    /**
     * 判断当前策略是否为严格阻断熔断模式。
     *
     * @return 若策略为 FAIL 或 FAIL_CLOSED 返回 true
     */
    public boolean failClosed() {
        return this == FAIL || this == FAIL_CLOSED;
    }
}

