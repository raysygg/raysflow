package com.acme.agentstudio.domain.knowledge.model;

/**
 * 知识检索中单个召回通道的具体执行细节与降级隔离事实 Record（Retrieval Channel Execution）。
 * 包含通道类型 channel (RetrievalChannel)、通道执行状态 status (RetrievalChannelExecutionStatus)、
 * 召回物理候选数量 candidateCount 及失败降级原因 failureReason (RetrievalChannelFailureReason)。
 *
 * @param channel 召回通道类型
 * @param status 通道执行状态
 * @param candidateCount 本通道召回切片数
 * @param failureReason 失败/跳过原因
 */
public record RetrievalChannelExecution(
        RetrievalChannel channel,
        RetrievalChannelExecutionStatus status,
        int candidateCount,
        RetrievalChannelFailureReason failureReason
) {
    /** 紧凑构造函数做输入校验与默认值兜底设置 */
    public RetrievalChannelExecution {
        channel = (channel == null) ? RetrievalChannel.VECTOR : channel;
        status = (status == null) ? RetrievalChannelExecutionStatus.SKIPPED : status;
        candidateCount = Math.max(0, candidateCount);
        failureReason = (failureReason == null) ? RetrievalChannelFailureReason.NONE : failureReason;
    }

    /**
     * 快捷创建通道执行成功记录。
     *
     * @param channel 目标通道
     * @param candidateCount 召回候选数
     * @return RetrievalChannelExecution 对象
     */
    public static RetrievalChannelExecution success(RetrievalChannel channel, int candidateCount) {
        return new RetrievalChannelExecution(channel, RetrievalChannelExecutionStatus.SUCCESS,
                candidateCount, RetrievalChannelFailureReason.NONE);
    }

    /**
     * 快捷创建通道执行失败（Provider 异常）记录。
     *
     * @param channel 目标通道
     * @return RetrievalChannelExecution 对象
     */
    public static RetrievalChannelExecution failed(RetrievalChannel channel) {
        return new RetrievalChannelExecution(channel, RetrievalChannelExecutionStatus.FAILED,
                0, RetrievalChannelFailureReason.PROVIDER_FAILURE);
    }

    /**
     * 快捷创建通道被跳过记录。
     *
     * @param channel 目标通道
     * @return RetrievalChannelExecution 对象
     */
    public static RetrievalChannelExecution skipped(RetrievalChannel channel) {
        return new RetrievalChannelExecution(channel, RetrievalChannelExecutionStatus.SKIPPED,
                0, RetrievalChannelFailureReason.QUERY_FEATURE_EMPTY);
    }
}

