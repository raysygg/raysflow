package com.acme.agentstudio.application.saas;

/**
 * 计费与财务系统集成适配器（Billing Integration Adapter） SPI 接口。
 * 负责定义对接外部 Stripe/微信支付/支付宝/企业 ERP/电子发票系统的开票与结算账单同步契约，隔离具体供应商实现细节。
 */
public interface BillingIntegrationAdapter {

    /**
     * 获取当前计费适配器的类型编码。
     *
     * @return 适配器类型（如 STRIPE / WECHAT_PAY / ENTERPRISE_ERP）
     */
    String type();

    /**
     * 向外部财务/开票系统提交账单同步申请。
     *
     * @param request 计费同步请求对象
     * @return 同步结果响应
     */
    BillingSyncResult submit(BillingSyncRequest request);

    /**
     * 计费账单同步请求参数 Record。
     *
     * @param tenantId 租户 ID
     * @param invoiceId 平台账单 ID
     * @param invoiceNumber 发票/账单单号
     * @param currency 结算币种
     * @param idempotencyKey 幂等校验 Key
     */
    record BillingSyncRequest(
            Long tenantId,
            Long invoiceId,
            String invoiceNumber,
            String currency,
            String idempotencyKey
    ) { }

    /**
     * 计费同步结果响应 Record。
     *
     * @param status 同步状态
     * @param externalReference 外部财务系统流水单号
     * @param safeSummary 脱敏后的结果摘要
     */
    record BillingSyncResult(
            SyncStatus status,
            String externalReference,
            String safeSummary
    ) { }

    /**
     * 计费同步状态枚举。
     */
    enum SyncStatus {

        /** 处理中/待确认 */
        PENDING,

        /** 确认成功/开票完成 */
        CONFIRMED,

        /** 状态未知/需手工对账 */
        UNKNOWN,

        /** 同步失败 */
        FAILED
    }
}

