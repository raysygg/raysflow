package com.acme.agentstudio.domain.knowledge.model;

/**
 * 知识库文档列表服务端物理分页条件查询请求实体 Record（Knowledge Document Page Query）。
 * 集中管理页码 page (1 起始)、每页数量 size (1~100 自动收敛)、标题/关键词 keyword、
 * 语种筛选条件 language (KnowledgeLanguage) 及解析构建状态 status。
 *
 * @param page 查询目标页码
 * @param size 每页显示记录上限数量
 * @param keyword 标题或正文搜索关键字
 * @param language 语种枚举筛选
 * @param status 状态筛选
 */
public record KnowledgeDocumentPageQuery(
        int page,
        int size,
        String keyword,
        KnowledgeLanguage language,
        String status
) {
    /** 默认起始页码：1 */
    public static final int DEFAULT_PAGE = 1;

    /** 默认每页条数：20 */
    public static final int DEFAULT_SIZE = 20;

    /** 允许的最大每页条数：100 */
    public static final int MAX_SIZE = 100;

    /** 紧凑构造函数做物理边界校验与字符串清洗 */
    public KnowledgeDocumentPageQuery {
        page = Math.max(DEFAULT_PAGE, page);
        size = Math.max(1, Math.min(MAX_SIZE, size));
        keyword = normalize(keyword);
        status = normalize(status);
    }

    /**
     * 计算数据库 SQL 查询所需的 OFFSET 偏移量。
     *
     * @return offset 计算结果 integer
     */
    public int offset() {
        return (page - 1) * size;
    }

    private static String normalize(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}

