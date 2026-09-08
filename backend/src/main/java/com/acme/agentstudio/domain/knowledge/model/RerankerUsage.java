package com.acme.agentstudio.domain.knowledge.model;

/**
 * 重排模型 Reranker 调用 Token 消费用量细粒度统计实体 Record（Reranker Usage）。
 * 包含输入 Token 数 inputTokens、输出 Token 数 outputTokens 及是否为估算值标志 estimated。
 *
 * @param inputTokens 输入 Token 消费数
 * @param outputTokens 输出 Token 消费数
 * @param estimated 是否为推算预估值（若供应商未直接返回）
 */
public record RerankerUsage(
        long inputTokens,
        long outputTokens,
        boolean estimated
) {
    /** 紧凑构造函数进行数值防负校准 */
    public RerankerUsage {
        inputTokens = Math.max(0L, inputTokens);
        outputTokens = Math.max(0L, outputTokens);
    }

    /**
     * 构建未知/估算全零用量实例。
     *
     * @return 标记为 estimated 的 0 用量 RerankerUsage 对象
     */
    public static RerankerUsage unknown() {
        return new RerankerUsage(0L, 0L, true);
    }
}

