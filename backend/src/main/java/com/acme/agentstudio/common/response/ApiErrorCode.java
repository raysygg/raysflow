package com.acme.agentstudio.common.response;

/**
 * 全局统一错误编码定义类。
 * 跨页面和跨服务共享的错误编码，展示文案统一由 ApiResponse 的 message 属性提供。
 */
public final class ApiErrorCode {

    /** 请求参数非法或校验未通过 (400) */
    public static final String BAD_REQUEST = "BAD_REQUEST";

    /** 字段或请求体校验失败 */
    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";

    /** 身份未认证或 Token 已失效 (401) */
    public static final String UNAUTHORIZED = "UNAUTHORIZED";

    /** 登录状态已失效且无法恢复 */
    public static final String AUTHENTICATION_EXPIRED = "AUTHENTICATION_EXPIRED";

    /** 当前角色无权访问该资源或执行该操作 (403) */
    public static final String FORBIDDEN = "FORBIDDEN";

    /** 请求中的租户上下文无效或已失效 */
    public static final String TENANT_CONTEXT_INVALID = "TENANT_CONTEXT_INVALID";

    /** 外部依赖暂时不可用 */
    public static final String DEPENDENCY_UNAVAILABLE = "DEPENDENCY_UNAVAILABLE";

    /** 请求的目标资源不存在 (404) */
    public static final String NOT_FOUND = "NOT_FOUND";

    /** 业务状态或版本编排存在并发冲突 (409) */
    public static final String CONFLICT = "CONFLICT";

    /** 目标运行版本停用或暂不可用 (503) */
    public static final String VERSION_UNAVAILABLE = "VERSION_UNAVAILABLE";

    /** 实时通信链路或服务暂不可用 (503) */
    public static final String REALTIME_UNAVAILABLE = "REALTIME_UNAVAILABLE";

    /** 知识向量模型服务未响应或调用失败 (503) */
    public static final String RAG_EMBEDDING_UNAVAILABLE = "RAG_EMBEDDING_UNAVAILABLE";

    /** 数据迁移进行中，当前入口处于只读锁定状态 */
    public static final String MIGRATION_READ_ONLY = "MIGRATION_READ_ONLY";

    /** 服务器内部处理错误 (500) */
    public static final String SERVER_ERROR = "SERVER_ERROR";

    /** 私有构造方法，防止实例化 */
    private ApiErrorCode() {
    }
}

