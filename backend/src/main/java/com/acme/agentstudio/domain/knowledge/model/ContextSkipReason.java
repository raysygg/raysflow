package com.acme.agentstudio.domain.knowledge.model;

/**
 * 父段落或知识切块未成功拼接进入最终 Context 的具体原因枚举（Context Skip Reason）。
 */
public enum ContextSkipReason {

    /** 超出 Token 预算限制上限 */
    TOKEN_BUDGET,

    /** 文本内容为空 */
    EMPTY_CONTENT
}

