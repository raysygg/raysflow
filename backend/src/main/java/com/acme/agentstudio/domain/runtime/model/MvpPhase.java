package com.acme.agentstudio.domain.runtime.model;

/**
 * 平台功能交付演进里程碑阶段枚举（MVP Phase）。
 */
public enum MvpPhase {

    /** 阶段 1：基础编排与单机对话运行 */
    MVP_1,

    /** 阶段 2：RAG 混合检索与模型能力路由 */
    MVP_2,

    /** 阶段 3：多 Agent 协作编排与离线评测门禁 */
    MVP_3,

    /** 阶段 4：企业级身份 SSO、SaaS 用量计费与数据治理 */
    MVP_4,

    /** 阶段 5：全功能生产观测与全链路自动化运维 */
    MVP_5
}

