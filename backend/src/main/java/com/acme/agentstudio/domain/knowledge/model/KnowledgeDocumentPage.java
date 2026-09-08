package com.acme.agentstudio.domain.knowledge.model;

import java.util.List;

/**
 * 知识库文档列表服务端物理分页统一结果容器 Record（Knowledge Document Page）。
 * 包含当页文档数据列表 items (List&lt;KnowledgeDocumentPageItem&gt;)、符合条件的总文档数 total、
 * 当前页码 page (1-indexed)、每页条数 size 及总页数 totalPages。
 *
 * @param items 当页文档列表项
 * @param total 满足条件的全局文档总条数
 * @param page 当前页码（1 起始）
 * @param size 每页记录数上限
 * @param totalPages 自动计算的总页数
 */
public record KnowledgeDocumentPage(
        List<KnowledgeDocumentPageItem> items,
        long total,
        int page,
        int size,
        int totalPages
) {
    /** 紧凑构造函数做输入数组防空保护 */
    public KnowledgeDocumentPage {
        items = (items == null) ? List.of() : List.copyOf(items);
    }

    /**
     * 根据查询条件和总条数快速构建物理分页结果。
     *
     * @param items 当页数据列表
     * @param total 总数据条数
     * @param query 分页查询请求参数对象
     * @return KnowledgeDocumentPage 分页结果实例
     */
    public static KnowledgeDocumentPage of(
            List<KnowledgeDocumentPageItem> items,
            long total,
            KnowledgeDocumentPageQuery query
    ) {
        int pages = (total == 0) ? 0 : (int) Math.ceil((double) total / query.size());
        return new KnowledgeDocumentPage(items, total, query.page(), query.size(), pages);
    }
}

