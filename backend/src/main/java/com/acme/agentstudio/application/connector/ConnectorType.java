package com.acme.agentstudio.application.connector;

/**
 * 外部工具与服务连接器协议类型枚举（Connector Type）。
 * 规定了工具节点（Tool Node）支持的物理传输协议或服务能力集。
 */
public enum ConnectorType {

    /** 标准 Swagger / OpenAPI 规范定义连接器 */
    OPENAPI,

    /** 通用 HTTP REST API 连接器 */
    HTTP,

    /** Webhook 异步回调通知连接器 */
    WEBHOOK,

    /** Anthropic Model Context Protocol (MCP) 上下文与工具协议连接器 */
    MCP,

    /** 平台内部微服务 API 连接器 */
    INTERNAL_API,

    /** Python / JavaScript 云端代码沙箱执行连接器 */
    CODE_SANDBOX
}

