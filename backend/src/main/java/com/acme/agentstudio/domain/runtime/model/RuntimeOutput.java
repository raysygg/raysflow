package com.acme.agentstudio.domain.runtime.model;

import java.util.List;
import java.util.Map;

/**
 * Agent Runtime 任务执行的标准化统一输出载荷 Record（Runtime Output）。
 * 统一承载主响应文本 text、结构化输出 Map structured、引用追溯列表 citations 及是否为最终答复标记 finalAnswer。
 *
 * @param text 交付给用户/前端的主生成文本
 * @param structured 经过 JSON Schema 强类型约束解析后的结构化数据 Map
 * @param citations RAG 检索可追溯引用的文献节点列表 List&lt;Citation&gt;
 * @param finalAnswer 是否为完整流程的最终答复（true 表示任务已收敛结束）
 */
public record RuntimeOutput(
        String text,
        Map<String, Object> structured,
        List<Citation> citations,
        boolean finalAnswer
) {
    /** 紧凑构造函数做防空保护 */
    public RuntimeOutput {
        text = (text == null) ? "" : text;
        structured = (structured == null) ? Map.of() : Map.copyOf(structured);
        citations = (citations == null) ? List.of() : List.copyOf(citations);
    }

    /**
     * RAG 知识库检索追溯引用实体 Record。
     *
     * @param sourceId 知识切片/源文档 ID
     * @param title 文档标题
     * @param excerpt 引用出的文段摘录
     * @param score 向量与重排器打分相关度得分
     */
    public record Citation(
            String sourceId,
            String title,
            String excerpt,
            double score
    ) {
        public Citation {
            if (sourceId == null || sourceId.isBlank()) {
                throw new IllegalArgumentException("引用来源标识不能为空");
            }
        }
    }
}

