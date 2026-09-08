package com.acme.agentstudio.application.knowledge;

import com.acme.agentstudio.domain.knowledge.KnowledgeLifecycleContracts.SourceType;

import java.util.List;

/**
 * 知识库数据源读取统一适配接口（Knowledge Source Adapter）。
 * 为本地文件上传、飞书/语雀/Notion/S3/Web Crawler 等异构外部数据源提供统一的游标分页拉取（readPage）契约。
 */
public interface KnowledgeSourceAdapter {

    /**
     * 获取当前适配器支持的数据源类型。
     *
     * @return 数据源类型枚举
     */
    SourceType sourceType();

    /**
     * 按游标增量拉取上一页或最新变更的文档数据列表。
     *
     * @param request 数据源读取请求
     * @return 分页文档数据包
     */
    SourcePage readPage(SourceRequest request);

    /** 数据源读取请求入参契约 Record */
    record SourceRequest(Long tenantId, Long sourceId, String cursor, int pageSize) {
    }

    /** 数据源分页响应数据包契约 Record */
    record SourcePage(String nextCursor, boolean hasMore, List<SourceDocument> documents) {
        public SourcePage {
            documents = documents == null ? List.of() : List.copyOf(documents);
        }
    }

    /** 数据源文档元数据与正文契约 Record */
    record SourceDocument(String externalId, String title, String content,
                          String contentType, String language, String permissionSummary) {
    }
}

