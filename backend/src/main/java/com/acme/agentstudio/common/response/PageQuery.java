package com.acme.agentstudio.common.response;

/**
 * 统一列表查询及分页参数实体 Record（Page Query）。
 * 用于规范前端分页、关键字搜索以及字段排序参数，避免不同页面重复约定分页逻辑。
 *
 * @param page 当前页码（从 1 开始）
 * @param size 每页记录条数（范围 10-100）
 * @param keyword 搜索关键字（模糊匹配名称、编码等）
 * @param sortBy 排序字段名称
 * @param sortDirection 排序方向（ASC 或 DESC）
 */
public record PageQuery(
        int page,
        int size,
        String keyword,
        String sortBy,
        String sortDirection
) {


    /** 默认起始页码：第 1 页 */
    public static final int FIRST_PAGE = 1;

    /** 默认每页记录数：20 条 */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /** 最小每页记录数：10 条 */
    public static final int MIN_PAGE_SIZE = 10;

    /** 最大每页记录数：100 条 */
    public static final int MAX_PAGE_SIZE = 100;

    /** 升序排序标识 */
    public static final String SORT_ASC = "ASC";

    /** 降序排序标识 */
    public static final String SORT_DESC = "DESC";

    /**
     * 规范化分页参数构造方法，防止非法页码与超大 size 导致的性能隐患。
     */
    public PageQuery {
        page = Math.max(FIRST_PAGE, page);
        size = Math.min(MAX_PAGE_SIZE, Math.max(MIN_PAGE_SIZE, size));
        sortDirection = SORT_DESC.equalsIgnoreCase(sortDirection) ? SORT_DESC : SORT_ASC;
    }

    /**
     * 获取默认配置的分页查询对象。
     *
     * @return 默认分页查询参数实例
     */
    public static PageQuery defaults() {
        return new PageQuery(FIRST_PAGE, DEFAULT_PAGE_SIZE, null, null, SORT_DESC);
    }

    /**
     * 计算 SQL 查询中的数据库偏置量（OFFSET）。
     *
     * @return 偏置记录数
     */
    public long offset() {
        return (long) (page - FIRST_PAGE) * size;
    }
}

