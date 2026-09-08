package com.acme.agentstudio.domain.knowledge;

import java.time.LocalDateTime;

/**
 * 企业知识库健康状态诊断与质量大盘摘要实体 Record（Knowledge Health Summary）。
 * 区分活跃知识源数 activeSources、过期滞后知识源数 staleSources、阻塞性异常数 openBlockers、
 * 告警提示数 openWarnings、在线生效索引版本数 activeGenerations 及计算时刻 calculatedAt。
 *
 * @param activeSources 正常在线生效的知识源数量
 * @param staleSources 超过更新阈值需要重新同步/索引的陈旧知识源数量
 * @param openBlockers 导致知识切块/向量化终止的严重错误告警数
 * @param openWarnings 可自动容错的警告统计数
 * @param activeGenerations 在线活动中的 Generation 索引版本总数
 * @param calculatedAt 健康指标计算生成时间
 */
public record KnowledgeHealthSummary(
        int activeSources,
        int staleSources,
        int openBlockers,
        int openWarnings,
        int activeGenerations,
        LocalDateTime calculatedAt
) {
}

