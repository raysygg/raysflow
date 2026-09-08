package com.acme.agentstudio.domain.knowledge.model;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 单个 RAG 知识检索命中的文本切片结果实体 Record（RAG Search Result）。
 * 包含切片正文 text、匹配相关度得分 score、来源文档标题/文件名 source、文档 ID documentId、
 * 起始 Chunk 序号 chunkStart、终止 Chunk 序号 chunkEnd、切块列表 chunkNumbers (List&lt;Integer&gt;)、
 * 识别语种 language (KnowledgeLanguage)、主检索渠道 channel (RetrievalChannel)、
 * 命中渠道集合 channels (List&lt;RetrievalChannel&gt;) 及命中原因列表 hitReasons (List&lt;RetrievalHitReason&gt;)。
 *
 * @param text 命中的切块/拼接父段落正文
 * @param score 相关度/重排得分
 * @param source 文档来源标题或文件名
 * @param documentId 归属文档数据库 ID
 * @param chunkStart 起始 Chunk 物理序号
 * @param chunkEnd 终止 Chunk 物理序号
 * @param chunkNumbers 包含的 Chunk 序号列表
 * @param language 识别出的语种类型
 * @param channel 主检索渠道（VECTOR / SPARSE / HYBRID）
 * @param channels 所有命中的多路渠道集合
 * @param hitReasons 检索命中具体原因说明列表
 */
public record RagSearchResult(
        String text,
        double score,
        String source,
        Long documentId,
        int chunkStart,
        int chunkEnd,
        List<Integer> chunkNumbers,
        KnowledgeLanguage language,
        RetrievalChannel channel,
        List<RetrievalChannel> channels,
        List<RetrievalHitReason> hitReasons
) {
    /**
     * 基础构造函数，快速构造单渠道命中结果。
     *
     * @param text 正文
     * @param score 得分
     * @param source 来源
     * @param documentId 文档 ID
     * @param chunkStart 起始序号
     * @param chunkEnd 终止序号
     * @param chunkNumbers 序号列表
     * @param language 语种
     * @param channel 检索渠道
     */
    public RagSearchResult(
            String text,
            double score,
            String source,
            Long documentId,
            int chunkStart,
            int chunkEnd,
            List<Integer> chunkNumbers,
            KnowledgeLanguage language,
            RetrievalChannel channel
    ) {
        this(text, score, source, documentId, chunkStart, chunkEnd, chunkNumbers, language, channel,
                (channel == null ? List.of() : List.of(channel)), List.of());
    }

    /** 紧凑构造函数做输入防空与默认主渠道判空填充 */
    public RagSearchResult {
        chunkNumbers = (chunkNumbers == null) ? List.of() : List.copyOf(chunkNumbers);
        language = (language == null) ? KnowledgeLanguage.OTHER : language;
        channels = (channels == null) ? List.of() : List.copyOf(channels);
        hitReasons = (hitReasons == null) ? List.of() : List.copyOf(hitReasons);
        channel = (channel == null)
                ? (channels.isEmpty() ? RetrievalChannel.VECTOR : channels.get(0))
                : channel;
    }

    /**
     * 构建友好展示给用户或前端引用的来源文本说明字符串。
     *
     * @return 格式化后的来源与切片范围描述字符串
     */
    public String sourceLabel() {
        String sourceName = (source == null || source.isBlank()) ? "未知来源" : source;
        if (chunkStart <= 0) {
            return sourceName;
        }
        String range = chunkNumbers.isEmpty()
                ? (chunkStart == chunkEnd ? String.valueOf(chunkStart) : chunkStart + "-" + chunkEnd)
                : chunkNumbers.stream().map(String::valueOf).collect(Collectors.joining(","));
        return sourceName + "，片段 " + range;
    }
}

