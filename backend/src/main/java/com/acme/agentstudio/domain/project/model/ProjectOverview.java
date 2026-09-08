package com.acme.agentstudio.domain.project.model;

import java.util.Map;

/**
 * 项目总览与阶段性指标概览 Record（Project Overview）。
 *
 * @param projectName 项目名称
 * @param metrics 项目核心统计指标 Map（包含应用数、知识库数、当月调用量等）
 */
public record ProjectOverview(
        String projectName,
        Map<String, Long> metrics
) {
}

