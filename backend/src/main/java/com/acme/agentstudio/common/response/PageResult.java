package com.acme.agentstudio.common.response;

import java.util.List;

/**
 * 统一分页响应结果包装实体 Record（Page Result）。
 * 明确约束数据列表 items (List&lt;T&gt;)、总数 total、页码 page、每页大小 size 以及是否有下一页 hasNext。
 *
 * @param <T> 数据记录类型
 * @param items 当前页的数据记录列表
 * @param total 满足查询条件的总记录数
 * @param page 当前页码
 * @param size 每页记录大小
 * @param hasNext 是否存在下一页
 */
public record PageResult<T>(
        List<T> items,
        long total,
        int page,
        int size,
        boolean hasNext
) {


    /**
     * 规范化构造校验，保障列表不为 null 且总数非负。
     */
    public PageResult {
        items = items == null ? List.of() : List.copyOf(items);
        if (total < 0) {
            throw new IllegalArgumentException("分页总数不能为负数");
        }
    }

    /**
     * 根据列表数据、总记录数和 PageQuery 分页查询对象快捷构造 PageResult。
     *
     * @param items 结果数据列表
     * @param total 总记录数
     * @param query 当前分页查询参数
     * @param <T> 数据元素类型
     * @return 分页响应结果对象
     */
    public static <T> PageResult<T> of(List<T> items, long total, PageQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("分页查询参数不能为空");
        }
        List<T> safeItems = items == null ? List.of() : items;
        return new PageResult<>(safeItems, total, query.page(), query.size(),
                query.offset() + safeItems.size() < total);
    }
}

