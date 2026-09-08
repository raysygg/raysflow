package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.knowledge.model.RagRetrievalRequest;
import com.acme.agentstudio.domain.knowledge.model.RagSearchResult;
import com.acme.agentstudio.domain.knowledge.model.RetrievalLanguageStrategy;
import com.acme.agentstudio.domain.knowledge.model.RetrievalScopeType;
import com.acme.agentstudio.domain.runtime.model.RetrievalCitation;
import com.acme.agentstudio.domain.runtime.model.RetrievalPolicy;
import com.acme.agentstudio.infrastructure.rag.RagRetrievalService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 运行时 RAG 检索策略执行与引用转换服务（Retrieval Policy Service）。
 * 将底层的 RagRetrievalService 检索能力适配归一化为运行时标准的 RetrievalPolicy 策略执行：
 * 包含搜索 Query 规范化、上下文字符数上界过滤（maxContextCharacters）、来源去重（Deduplication）、
 * 引用引用块抽取（Citations）以及未命中兜底消息处理（No-Hit Message）。
 */
@Service
public class RetrievalPolicyService {

    /** 基础 RAG 检索服务 */
    private final RagRetrievalService retrievalService;

    /**
     * 构造函数注入 RAG 检索服务。
     *
     * @param retrievalService 基础 RAG 检索服务 RagRetrievalService
     */
    public RetrievalPolicyService(RagRetrievalService retrievalService) {
        this.retrievalService = retrievalService;
    }

    /**
     * 根据运行时检索策略，向底层知识库发起检索并转换结果与可追溯引用。
     *
     * @param tenantId 租户 ID
     * @param query 原始用户查询 Query
     * @param documentId 指定包含的单一文档 ID（可选）
     * @param policy 检索策略参数实体 RetrievalPolicy
     * @return 运行时检索结果实体 RetrievalResult
     */
    public RetrievalResult retrieve(long tenantId, String query, Long documentId, RetrievalPolicy policy) {
        RetrievalPolicy effective = (policy == null) ? RetrievalPolicy.defaults() : policy;
        String normalizedQuery = normalizeQuery(query);
        List<Long> documentIds = (documentId == null) ? List.of() : List.of(documentId);

        List<RagSearchResult> raw = retrievalService.retrieveWithContext(new RagRetrievalRequest(
                tenantId,
                null,
                normalizedQuery,
                RetrievalLanguageStrategy.AUTO,
                null,
                (documentId == null) ? RetrievalScopeType.VISIBLE_DOCUMENTS : RetrievalScopeType.EXPLICIT_DOCUMENTS,
                documentIds,
                effective.maxResults(),
                null
        )).results();

        List<RagSearchResult> selected = select(raw, effective);
        List<RetrievalCitation> citations = effective.includeCitations() ? citations(selected) : List.of();

        return new RetrievalResult(
                normalizedQuery,
                selected,
                citations,
                selected.isEmpty(),
                selected.isEmpty() ? effective.noHitMessage() : ""
        );
    }

    /** 清洗规范查询文本 */
    private String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("检索问题输入 query 不能为空。");
        }
        return query.trim();
    }

    /** 依据去重策略与上下文最大字符数熔断筛选结果 */
    private List<RagSearchResult> select(List<RagSearchResult> raw, RetrievalPolicy policy) {
        Map<String, RagSearchResult> unique = new LinkedHashMap<>();
        int characters = 0;
        List<RagSearchResult> safeRaw = (raw == null) ? List.of() : raw;

        for (RagSearchResult item : safeRaw) {
            if (item == null) {
                continue;
            }
            String key = policy.deduplicateSources()
                    ? (item.documentId() + ":" + item.source())
                    : item.source();

            if (unique.containsKey(key)) {
                continue;
            }
            int itemLength = (item.text() == null) ? 0 : item.text().length();
            if (characters + itemLength > policy.maxContextCharacters()) {
                continue;
            }
            unique.put(key, item);
            characters += itemLength;
            if (unique.size() >= policy.maxResults()) {
                break;
            }
        }
        return new ArrayList<>(unique.values());
    }

    /** 将原始 RAG 检索命中的切块转换为前端可引用的 Citation */
    private List<RetrievalCitation> citations(List<RagSearchResult> results) {
        return results.stream()
                .map(item -> new RetrievalCitation(
                        String.valueOf(item.documentId()),
                        item.source(),
                        item.score(),
                        item.text()
                ))
                .toList();
    }

    /**
     * 运行时检索结果传输 Record。
     *
     * @param query 规范后的搜索词
     * @param results 命中的原始 RAG 切块结果列表
     * @param citations 转换的可追溯引用列表
     * @param noHit 是否完全未命中
     * @param noHitMessage 未命中时的提示兜底文案
     */
    public record RetrievalResult(
            String query,
            List<RagSearchResult> results,
            List<RetrievalCitation> citations,
            boolean noHit,
            String noHitMessage
    ) {
        public RetrievalResult {
            results = (results == null) ? List.of() : List.copyOf(results);
            citations = (citations == null) ? List.of() : List.copyOf(citations);
            noHitMessage = (noHitMessage == null) ? "" : noHitMessage;
        }
    }
}

