package com.acme.agentstudio.domain.knowledge.model;

import java.util.List;

/**
 * 组装拼接 RAG 上下文时因溢出或空内容被忽略跳过的父段落/切片摘要 Record（Skipped Context）。
 * 包含文档物理 ID documentId、父切块物理 ID parentChunkId、切块序号列表 chunkNumbers (List&lt;Integer&gt;)
 * 及跳过原因 reason (ContextSkipReason)。
 *
 * @param documentId 归属文档 ID
 * @param parentChunkId 被跳过的父切块 ID
 * @param chunkNumbers 包含的子切块序号列表
 * @param reason 跳过的具体原因（ContextSkipReason: TOKEN_BUDGET / EMPTY_CONTENT）
 */
public record SkippedContext(
        Long documentId,
        Long parentChunkId,
        List<Integer> chunkNumbers,
        ContextSkipReason reason
) {
    /** 紧凑构造函数做输入防空与默认原因初始化 */
    public SkippedContext {
        chunkNumbers = (chunkNumbers == null) ? List.of() : List.copyOf(chunkNumbers);
        reason = (reason == null) ? ContextSkipReason.EMPTY_CONTENT : reason;
    }
}

