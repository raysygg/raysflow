package com.acme.agentstudio.domain.runtime.model;

/**
 * 文本内容受信与指令注入风险安全标记枚举（Content Trust）。
 * 用于标记用户输入、检索结果（RAG Context）以及工具返回内容的安全可信级别，防御 Prompt 注入攻击。
 */
public enum ContentTrust {

    /** 完全受信（例如系统固化的提示词指令） */
    TRUSTED,

    /** 非受信（外部用户输入或未知网络抓取内容） */
    UNTRUSTED,

    /** 可疑注入风险（检测到敏感指令覆盖特征） */
    SUSPICIOUS
}

