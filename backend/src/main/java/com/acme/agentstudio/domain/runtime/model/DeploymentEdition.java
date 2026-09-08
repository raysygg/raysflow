package com.acme.agentstudio.domain.runtime.model;

/**
 * 平台部署版本架构形态枚举（Deployment Edition）。
 * 区分公有云 SaaS (CLOUD)、私有云 (PRIVATE_CLOUD)、本地机房 (ON_PREMISE) 与完全物理隔离物理离线 (OFFLINE)。
 */
public enum DeploymentEdition {

    /** 公有云 SaaS 多租户版 */
    CLOUD,

    /** 专有云/私有云托管版 */
    PRIVATE_CLOUD,

    /** 企业本地自建机房 On-Premise 部署版 */
    ON_PREMISE,

    /** 物理物理隔离断网离线环境部署版 */
    OFFLINE
}

